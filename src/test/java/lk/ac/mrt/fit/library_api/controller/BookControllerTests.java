package lk.ac.mrt.fit.library_api.controller;

import java.util.List;
import java.util.Optional;
import lk.ac.mrt.fit.library_api.model.Book;
import lk.ac.mrt.fit.library_api.service.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookControllerTests {
    private BookService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private Book book;

    @BeforeEach
    void setUp() {
        service = mock(BookService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new BookController(service))
                .setValidator(validator).build();
        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void createsBookAndReturnsGeneratedId() throws Exception {
        when(service.createBook(any(Book.class))).thenReturn(book);
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"));
        verify(service).createBook(any(Book.class));
    }

    @Test
    void listsBooksAndReturnsExistingBook() throws Exception {
        when(service.getAllBooks()).thenReturn(List.of(book));
        when(service.getBookById(1L)).thenReturn(Optional.of(book));
        mvc.perform(get("/api/books")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/books/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void updatesBook() throws Exception {
        book.setTitle("Updated");
        when(service.updateBook(eq(1L), any(Book.class))).thenReturn(Optional.of(book));
        mvc.perform(put("/api/books/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated\",\"author\":\"Robert C. Martin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deletesBookWithEmptyResponse() throws Exception {
        when(service.deleteBook(1L)).thenReturn(true);
        mvc.perform(delete("/api/books/1")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void missingRecordsReturn404() throws Exception {
        when(service.getBookById(99L)).thenReturn(Optional.empty());
        when(service.updateBook(eq(99L), any(Book.class))).thenReturn(Optional.empty());
        when(service.deleteBook(99L)).thenReturn(false);
        mvc.perform(get("/api/books/99")).andExpect(status().isNotFound());
        mvc.perform(put("/api/books/99").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Book\",\"author\":\"Author\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/books/99")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\"\",\"author\":\"Author\"}",
            "{\"title\":\"   \",\"author\":\"Author\"}",
            "{\"title\":\"Book\",\"author\":\"\"}",
            "{\"author\":\"Author\"}",
            "{\"title\":\"Book\",\"author\":\"Author\",\"price\":-1}"
    })
    void rejectsInvalidPostAndPutBeforeCallingService(String body) throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/books/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsZeroPriceAndOptionalIsbn() throws Exception {
        when(service.createBook(any(Book.class))).thenReturn(book);
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Book\",\"author\":\"Author\",\"price\":0}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
