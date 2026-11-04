package lk.ac.mrt.fit.library_api.service;

import java.util.List;
import java.util.Optional;
import lk.ac.mrt.fit.library_api.model.Book;
import lk.ac.mrt.fit.library_api.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTests {
    @Mock
    private BookRepository repository;
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService(repository);
    }

    @Test
    void createsBookWithGeneratedIdRatherThanClientId() {
        Book book = book("Clean Code", "Robert C. Martin", "9780132350884", 4500.0);
        book.setId(99L);
        when(repository.save(book)).thenAnswer(invocation -> {
            assertNull(book.getId());
            book.setId(1L);
            return book;
        });
        assertEquals(1L, service.createBook(book).getId());
        verify(repository).save(book);
    }

    @Test
    void listsAndRetrievesExistingBooks() {
        Book book = new Book();
        when(repository.findAll()).thenReturn(List.of(book));
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        assertEquals(List.of(book), service.getAllBooks());
        assertSame(book, service.getBookById(1L).orElseThrow());
    }

    @Test
    void returnsEmptyForMissingBook() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(service.getBookById(99L).isEmpty());
    }

    @Test
    void updatesEveryEditableFieldAndPreservesId() {
        Book existing = book("Old", "Old author", "Old ISBN", 1.0);
        existing.setId(1L);
        Book details = book("New", "New author", "New ISBN", 2.0);
        details.setId(99L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        Book result = service.updateBook(1L, details).orElseThrow();
        assertAll(() -> assertEquals(1L, result.getId()),
                () -> assertEquals("New", result.getTitle()),
                () -> assertEquals("New author", result.getAuthor()),
                () -> assertEquals("New ISBN", result.getIsbn()),
                () -> assertEquals(2.0, result.getPrice()));
        verify(repository).save(existing);
    }

    @Test
    void updateCanClearOptionalFields() {
        Book existing = book("Old", "Author", "ISBN", 10.0);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        Book result = service.updateBook(1L, book("New", "Author", null, null)).orElseThrow();
        assertNull(result.getIsbn());
        assertNull(result.getPrice());
    }

    @Test
    void missingUpdateDoesNotSave() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(service.updateBook(99L, new Book()).isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void deletesExistingBook() {
        when(repository.existsById(1L)).thenReturn(true);
        assertTrue(service.deleteBook(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void missingDeleteDoesNotDelete() {
        when(repository.existsById(99L)).thenReturn(false);
        assertFalse(service.deleteBook(99L));
        verify(repository, never()).deleteById(anyLong());
    }

    private Book book(String title, String author, String isbn, Double price) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setPrice(price);
        return book;
    }
}
