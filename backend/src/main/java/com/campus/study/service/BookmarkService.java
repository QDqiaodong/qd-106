package com.campus.study.service;

import com.campus.study.entity.Bookmark;
import com.campus.study.repository.BookmarkRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookmarkService {

    @Resource
    private BookmarkRepository bookmarkRepository;

    public List<Bookmark> getBookmarks(Long userId, Long materialId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return bookmarkRepository.findByUserIdAndMaterialId(userId, materialId, sort);
    }

    public Bookmark addBookmark(Long userId, Long materialId, Integer pageNumber, String chapterName, String note) {
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setMaterialId(materialId);
        bookmark.setPageNumber(pageNumber);
        bookmark.setChapterName(chapterName);
        bookmark.setNote(note);
        return bookmarkRepository.save(bookmark);
    }

    public Bookmark updateBookmark(Long id, Long userId, Integer pageNumber, String chapterName, String note) {
        Optional<Bookmark> optional = bookmarkRepository.findByIdAndUserId(id, userId);
        if (optional.isEmpty()) {
            return null;
        }
        Bookmark bookmark = optional.get();
        if (pageNumber != null) {
            bookmark.setPageNumber(pageNumber);
        }
        if (chapterName != null) {
            bookmark.setChapterName(chapterName);
        }
        if (note != null) {
            bookmark.setNote(note);
        }
        return bookmarkRepository.save(bookmark);
    }

    @Transactional
    public boolean deleteBookmark(Long id, Long userId) {
        Optional<Bookmark> optional = bookmarkRepository.findByIdAndUserId(id, userId);
        if (optional.isEmpty()) {
            return false;
        }
        bookmarkRepository.deleteByIdAndUserId(id, userId);
        return true;
    }

    public Bookmark getBookmark(Long id, Long userId) {
        return bookmarkRepository.findByIdAndUserId(id, userId).orElse(null);
    }
}
