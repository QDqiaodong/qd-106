package com.campus.study.service;

import com.campus.study.entity.Material;
import com.campus.study.entity.TopicFolder;
import com.campus.study.entity.TopicFolderItem;
import com.campus.study.entity.User;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.repository.TopicFolderItemRepository;
import com.campus.study.repository.TopicFolderRepository;
import com.campus.study.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TopicFolderService {

    @Resource
    private TopicFolderRepository topicFolderRepository;

    @Resource
    private TopicFolderItemRepository topicFolderItemRepository;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private UserRepository userRepository;

    public Page<TopicFolder> getFolderPage(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "sort").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<TopicFolder> folderPage = topicFolderRepository.findByStatusOrderBySortAscCreatedAtDesc(1, pageable);
        enrichFolders(folderPage.getContent());
        return folderPage;
    }

    public List<TopicFolder> getAllFolders() {
        List<TopicFolder> folders = topicFolderRepository.findByStatusOrderBySortAscCreatedAtDesc(1);
        enrichFolders(folders);
        return folders;
    }

    public TopicFolder getFolderById(Long id) {
        TopicFolder folder = topicFolderRepository.findById(id).orElse(null);
        if (folder != null && folder.getStatus() == 1) {
            enrichFolder(folder);
        }
        return folder;
    }

    public List<TopicFolderItem> getFolderItems(Long folderId) {
        List<TopicFolderItem> items = topicFolderItemRepository.findByFolderIdOrderBySortAscIdAsc(folderId);
        if (items.isEmpty()) {
            return items;
        }
        List<Long> materialIds = items.stream().map(TopicFolderItem::getMaterialId).collect(Collectors.toList());
        List<Material> materials = materialRepository.findAllById(materialIds);
        Map<Long, Material> materialMap = new HashMap<>();
        for (Material m : materials) {
            materialMap.put(m.getId(), m);
        }
        for (TopicFolderItem item : items) {
            item.setMaterial(materialMap.get(item.getMaterialId()));
        }
        return items;
    }

    public TopicFolder getFolderDetailWithViewCount(Long id) {
        TopicFolder folder = getFolderById(id);
        if (folder != null) {
            topicFolderRepository.incrementViewCount(id);
            folder.setViewCount(folder.getViewCount() + 1);
        }
        return folder;
    }

    @Transactional
    public TopicFolder createFolder(String name, String description, Long userId, String cover, List<Map<String, Object>> items) {
        TopicFolder folder = new TopicFolder();
        folder.setName(name);
        folder.setDescription(description != null ? description : "");
        folder.setCover(cover != null ? cover : "");
        folder.setUserId(userId);
        folder.setSort(0);
        folder.setStatus(1);
        TopicFolder saved = topicFolderRepository.save(folder);

        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> itemData = items.get(i);
                TopicFolderItem item = new TopicFolderItem();
                item.setFolderId(saved.getId());
                item.setMaterialId(Long.valueOf(itemData.get("materialId").toString()));
                item.setSort(i + 1);
                item.setRemark(itemData.get("remark") != null ? itemData.get("remark").toString() : "");
                topicFolderItemRepository.save(item);
            }
        }

        int count = topicFolderItemRepository.countByFolderId(saved.getId());
        saved.setMaterialCount(count);
        return saved;
    }

    @Transactional
    public TopicFolder updateFolder(Long id, String name, String description, String cover, List<Map<String, Object>> items, Long userId) {
        TopicFolder folder = topicFolderRepository.findById(id).orElse(null);
        if (folder == null) {
            return null;
        }
        if (!folder.getUserId().equals(userId)) {
            return null;
        }
        if (name != null) {
            folder.setName(name);
        }
        if (description != null) {
            folder.setDescription(description);
        }
        if (cover != null) {
            folder.setCover(cover);
        }
        TopicFolder saved = topicFolderRepository.save(folder);

        if (items != null) {
            topicFolderItemRepository.deleteByFolderId(id);
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> itemData = items.get(i);
                TopicFolderItem item = new TopicFolderItem();
                item.setFolderId(id);
                item.setMaterialId(Long.valueOf(itemData.get("materialId").toString()));
                item.setSort(i + 1);
                item.setRemark(itemData.get("remark") != null ? itemData.get("remark").toString() : "");
                topicFolderItemRepository.save(item);
            }
        }

        int count = topicFolderItemRepository.countByFolderId(id);
        saved.setMaterialCount(count);
        return saved;
    }

    @Transactional
    public boolean deleteFolder(Long id, Long userId) {
        TopicFolder folder = topicFolderRepository.findById(id).orElse(null);
        if (folder == null) {
            return false;
        }
        if (!folder.getUserId().equals(userId)) {
            return false;
        }
        folder.setStatus(0);
        topicFolderRepository.save(folder);
        return true;
    }

    public Page<TopicFolder> getMyFolders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TopicFolder> folderPage = topicFolderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, 1, pageable);
        enrichFolders(folderPage.getContent());
        return folderPage;
    }

    private void enrichFolders(List<TopicFolder> folders) {
        if (folders == null || folders.isEmpty()) {
            return;
        }
        List<Long> folderIds = folders.stream().map(TopicFolder::getId).collect(Collectors.toList());
        Map<Long, Integer> countMap = new HashMap<>();
        for (Long fid : folderIds) {
            countMap.put(fid, topicFolderItemRepository.countByFolderId(fid));
        }
        for (TopicFolder folder : folders) {
            folder.setMaterialCount(countMap.getOrDefault(folder.getId(), 0));
        }

        List<Long> userIds = folders.stream().map(TopicFolder::getUserId).distinct().collect(Collectors.toList());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> userNameMap = new HashMap<>();
        for (User user : users) {
            userNameMap.put(user.getId(), user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername());
        }
        for (TopicFolder folder : folders) {
            folder.setCreatorName(userNameMap.getOrDefault(folder.getUserId(), "未知用户"));
        }
    }

    private void enrichFolder(TopicFolder folder) {
        List<TopicFolder> list = new ArrayList<>();
        list.add(folder);
        enrichFolders(list);
    }
}
