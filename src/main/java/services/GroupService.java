package services;

import models.dao.GroupDao;
import models.hibernate.utils.GroupCriteria;
import models.hibernate.utils.GroupType;
import repositories.GroupRepository;
import repositories.utils.CountGroupDao;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GroupService {
    private final Collection<Long> activeGroupIds;
    private final Collection<Long> inactiveGroupIds;

    private final GroupRepository groupRepository;
    private final Map<Long, GroupDao> groupInfoMap;
    private final Map<Long, Map<UUID, Integer>> groupTripMessageMap;

    private GroupService(Collection<Long> activeGroupIds, Collection<Long> inactiveGroupIds,
                         Map<Long, GroupDao> groupInfoMap, Map<Long, Map<UUID, Integer>> groupTripMessageMap) {
        this.activeGroupIds = activeGroupIds;
        this.inactiveGroupIds = inactiveGroupIds;
        this.groupInfoMap = groupInfoMap;

        this.groupRepository = new GroupRepository();
        this.groupTripMessageMap = groupTripMessageMap;
    }

    private static GroupService INSTANCE;

    public static GroupService getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("Instance has not been initialized");
        return INSTANCE;
    }

    public static void initializeInstance(Collection<Long> activeGroupIds, Collection<Long> inactiveGroupIds
            , Map<Long, GroupDao> groupInfoMap, Map<Long, Map<UUID, Integer>> groupTripMessageMap) {
        if (INSTANCE != null)
            throw new RuntimeException("Instance has already been initialized");
        INSTANCE = new GroupService(activeGroupIds, inactiveGroupIds, groupInfoMap, groupTripMessageMap);
    }

    public void addNewGroup(long chatId, String groupName) {
        // if group already saved, then only update name,
        // otherwise set active
        if (activeGroupIds.contains(chatId) || inactiveGroupIds.contains(chatId)) {
            putGroupName(chatId, groupName);
            return;
        }
        putGroupName(chatId, groupName);
        activeGroupIds.add(chatId);
    }

    public void addNewChannel(long channelId, String channelName) {
        // if group already saved, then only update name,
        // otherwise set active
        if (activeGroupIds.contains(channelId) || inactiveGroupIds.contains(channelId)) {
            putChannelName(channelId, channelName);
            return;
        }
        putChannelName(channelId, channelName);
        activeGroupIds.add(channelId);
    }

    public void removeIfActive(long chatId) {
        // if the group is banned, re-adding bot to chat won't give any result
        if (inactiveGroupIds.contains(chatId))
            return;
        activeGroupIds.remove(chatId);
        groupInfoMap.remove(chatId);
        groupTripMessageMap.remove(chatId);
        CompletableFuture.runAsync(() -> groupRepository.removeGroup(chatId));
    }

    public void setGroupActive(long groupId) {
        inactiveGroupIds.remove(groupId);
        activeGroupIds.add(groupId);
        CompletableFuture.runAsync(() -> groupRepository.setGroupActive(groupId));
    }

    public void setGroupInactive(long groupId) {
        activeGroupIds.remove(groupId);
        groupTripMessageMap.remove(groupId);
        inactiveGroupIds.add(groupId);
        CompletableFuture.runAsync(() -> groupRepository.setGroupInactive(groupId));
    }

    public List<Long> getActiveGroupIds() {
        return new LinkedList<>(activeGroupIds);
    }

    public Collection<Long> getInactiveGroupIds() {
        return new LinkedList<>(inactiveGroupIds);
    }

    public String getGroupName(long chatId) {
        GroupDao group = groupInfoMap.get(chatId);
        return group == null ? null : group.getGroupName();
    }

    public void putGroupName(long groupId, String groupName) {
        GroupDao group = groupInfoMap.computeIfAbsent(groupId, x -> new GroupDao(groupId, GroupType.GROUP));
        group.setGroupName(groupName);
        CompletableFuture.runAsync(() -> groupRepository.setGroupName(groupId, groupName));
    }

    public void putChannelName(long channelId, String channelName) {
        GroupDao channel = groupInfoMap.computeIfAbsent(channelId, x -> new GroupDao(channelId, GroupType.CHANNEL));
        channel.setGroupName(channelName);
        CompletableFuture.runAsync(() -> groupRepository.setChannelName(channelId, channelName));
    }

    public GroupType getGroupType(long chatId) {
        GroupDao group = groupInfoMap.get(chatId);
        return group == null ? null : group.getGroupType();
    }

    public CountGroupDao getAllGroups(Integer page, Integer limit, GroupCriteria groupCriteria) {
        return groupRepository.getAll(page, limit, groupCriteria);
    }

    public Integer getMessageIdByGroupAndTripId(long groupId, UUID tripId) {
        return groupTripMessageMap.computeIfAbsent(groupId, x -> new HashMap<>()).get(tripId);
    }

    public void deleteMessage(long groupId, UUID tripId) {
        groupTripMessageMap.computeIfAbsent(groupId, x -> new HashMap<>()).remove(tripId);
        CompletableFuture.runAsync(() -> groupRepository.removeMessageIdByGroupAndTrip(groupId, tripId));
    }

    public void setMessageIdByGroupAndTripId(long groupId, UUID tripId, Integer messageId) {
        groupTripMessageMap.computeIfAbsent(groupId, x -> new HashMap<>()).put(tripId, messageId);
        CompletableFuture.runAsync(() -> groupRepository.setMessageIdByGroupAndTrip(groupId, tripId, messageId));
    }
}
