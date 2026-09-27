package com.toodback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import com.toodback.member.MemberRepository;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-at-least-thirty-two-bytes-long",
        "spring.datasource.url=jdbc:h2:mem:board-api-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BoardApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired MemberRepository memberRepository;

    @Test
    void signupLoginAndValidation() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong\",\"password\":\"12345678\",\"nickname\":\"Kim\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"short\",\"nickname\":\"Kim\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));

        MvcResult signup = mvc.perform(post("/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"nickname\":\"Kim\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.encodedPassword").doesNotExist())
                .andReturn();
        assertThat(signup.getResponse().getContentAsString()).doesNotContain("password123");
        String storedPassword = memberRepository.findByEmail(email).orElseThrow().getEncodedPassword();
        assertThat(storedPassword).startsWith("$2").isNotEqualTo("password123");

        mvc.perform(post("/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"nickname\":\"Kim\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));

        String token = login(email);
        mvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void postCommentPermissionsAndDeletion() throws Exception {
        String author = signupAndLogin("Author");
        String other = signupAndLogin("Other");

        mvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\",\"content\":\"Body\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/posts").header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\",\"content\":\"Body\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));

        long postId = json(mvc.perform(post("/posts").header("Authorization", "Bearer " + author)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\",\"content\":\"Body\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        mvc.perform(put("/posts/{id}", postId).header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Changed\",\"content\":\"Body\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));

        long commentId = json(mvc.perform(post("/posts/{id}/comments", postId)
                        .header("Authorization", "Bearer " + author).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(put("/posts/{postId}/comments/{id}", postId, commentId)
                        .header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Changed\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/posts/{id}/comments", postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello"));
        mvc.perform(get("/posts/{id}", 999999)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mvc.perform(delete("/posts/{id}", postId).header("Authorization", "Bearer " + author))
                .andExpect(status().isNoContent());
        mvc.perform(get("/posts/{id}/comments", postId)).andExpect(status().isNotFound());
    }

    @Test
    void paginatedListIncludesCommentCountsWithoutPerPostQueries() throws Exception {
        String token = signupAndLogin("ListAuthor");
        for (int i = 0; i < 3; i++) {
            long postId = json(mvc.perform(post("/posts").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Post " + i + "\",\"content\":\"Body\"}"))
                    .andExpect(status().isCreated()).andReturn()).get("id").asLong();
            mvc.perform(post("/posts/{id}/comments", postId).header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Comment\"}"))
                    .andExpect(status().isCreated());
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        sessionFactory.getStatistics().clear();
        MvcResult result = mvc.perform(get("/posts?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].authorNickname").value("ListAuthor"))
                .andExpect(jsonPath("$.content[0].commentCount").value(1))
                .andReturn();
        assertThat(json(result).get("totalElements").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    void authorCanEditAndDeletePostsAndComments() throws Exception {
        String token = signupAndLogin("Editor");
        long postId = json(mvc.perform(post("/posts").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Original\",\"content\":\"Body\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        mvc.perform(put("/posts/{id}", postId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Edited\",\"content\":\"New body\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Edited"));
        mvc.perform(get("/posts/{id}", postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("New body"));

        long commentId = json(mvc.perform(post("/posts/{id}/comments", postId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Original comment\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(put("/posts/{postId}/comments/{id}", postId, commentId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Edited comment\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Edited comment"));
        mvc.perform(post("/posts/{id}/comments", postId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(delete("/posts/{postId}/comments/{id}", postId, commentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/posts/{id}/comments", postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/posts/{postId}/comments/{id}", postId, commentId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Gone\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(delete("/posts/{id}", postId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/posts/{id}", postId)).andExpect(status().isNotFound());
    }

    private String signupAndLogin(String nickname) throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json(result).get("accessToken").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
