package com.library.service;

import com.library.dto.BookDTO;
import com.library.entity.Book;
import com.library.entity.BookFavorite;
import com.library.entity.User;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BookFavoriteRepository;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class FavoriteService {

    @Autowired private BookFavoriteRepository favoriteRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookService bookService;

    /** Toggle a book's favorite state for the current user. Returns true if now favorited. */
    public boolean toggle(Long bookId) {
        Long uid = SecurityUtils.getCurrentUserId();
        var existing = favoriteRepository.findByUserIdAndBookId(uid, bookId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new UnauthorizedException("کاربر فعلی پیدا نشد"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتاب پیدا نشد"));
        favoriteRepository.save(BookFavorite.builder()
                .user(user).book(book).createdAt(LocalDateTime.now()).build());
        return true;
    }

    @Transactional(readOnly = true)
    public List<Long> getFavoriteBookIds() {
        return favoriteRepository.findBookIdsByUserId(SecurityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> getFavorites(String search, Pageable pageable) {
        Long uid = SecurityUtils.getCurrentUserId();
        final String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        Specification<BookFavorite> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("user").get("id"), uid));
            if (q != null) {
                var book = root.join("book");
                String pat = "%" + q + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(book.get("title")), pat),
                    cb.like(cb.lower(cb.coalesce(book.get("author"), "")), pat)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return favoriteRepository.findAll(spec, pageable).map(f -> bookService.mapToBookDTO(f.getBook()));
    }
}
