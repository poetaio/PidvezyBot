package services;

import models.hibernate.Group;
import models.hibernate.utils.GroupCriteria;
import models.utils.GroupStatus;
import repositories.GroupRepository;
import repositories.utils.CountGroupDao;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GroupService {
    private final Collection<Long> activeGroupIds;
    private final Collection<Long> inactiveGroupIds;

    private final GroupRepository groupRepository;
    private final Map<Long, String> groupNamesMap;
    private final Map<Long, Map<UUID, Integer>> groupTripMessageMap;

    private GroupService(Collection<Long> activeGroupIds, Collection<Long> inactiveGroupIds,
                         Map<Long, String> groupNamesMap, Map<Long, Map<UUID, Integer>> groupTripMessageMap) {
        this.activeGroupIds = activeGroupIds;
        this.inactiveGroupIds = inactiveGroupIds;
        this.groupNamesMap = groupNamesMap;

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
            , Map<Long, String> chatNamesMap, Map<Long, Map<UUID, Integer>> groupTripMessageMap) {
        INSTANCE = new GroupService(activeGroupIds, inactiveGroupIds, chatNamesMap, groupTripMessageMap);
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

    public void removeGroupIfActive(long chatId) {
        // if the group is banned, re-adding bot to chat won't give any result
        if (inactiveGroupIds.contains(chatId))
            return;
        activeGroupIds.remove(chatId);
        groupNamesMap.remove(chatId);
        CompletableFuture.runAsync(() -> groupRepository.removeGroup(chatId));
    }

    public void setGroupActive(long groupId) {
        inactiveGroupIds.remove(groupId);
        activeGroupIds.add(groupId);
        CompletableFuture.runAsync(() -> groupRepository.setGroupActive(groupId));
    }

    public void setGroupInactive(long groupId) {
        activeGroupIds.remove(groupId);
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
        return groupNamesMap.get(chatId);
    }

    public void putGroupName(long groupId, String groupName) {
        groupNamesMap.put(groupId, groupName);
        CompletableFuture.runAsync(() -> groupRepository.setName(groupId, groupName));
    }

    public CountGroupDao getAllGroups(Integer page, Integer limit, GroupCriteria groupCriteria) {
        return groupRepository.getAll(page, limit, groupCriteria);
    }

    public Integer getMessageIdByGroupAndTripId(long groupId, UUID tripId) {
        return groupTripMessageMap.computeIfAbsent(groupId, x -> new HashMap<>()).get(tripId);
    }

    public void setMessageIdByGroupAndTripId(long groupId, UUID tripId, Integer messageId) {
        groupTripMessageMap.computeIfAbsent(groupId, x -> new HashMap<>()).put(tripId, messageId);
        CompletableFuture.runAsync(() -> groupRepository.setMessageIdByGroupAndTrip(groupId, tripId, messageId));
    }
}
