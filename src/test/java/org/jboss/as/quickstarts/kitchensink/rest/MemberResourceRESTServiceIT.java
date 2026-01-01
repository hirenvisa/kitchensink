/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.kitchensink.rest;

import java.util.List;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Spring Boot integration tests for MemberResourceRESTService
 * Uses @SpringBootTest with MockMvc for REST API testing
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
public class MemberResourceRESTServiceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberResourceRESTService restService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRegistration memberRegistration;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testListAllMembers() throws Exception {
        // Create some test members
        Member member1 = createTestMember("Alice", "alice@rest.com", "1111111111");
        Member member2 = createTestMember("Bob", "bob@rest.com", "2222222222");

        memberRegistration.register(member1);
        memberRegistration.register(member2);

        // Test via REST endpoint
        mockMvc.perform(get("/rest/members"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        // Test via service directly
        List<Member> members = restService.listAllMembers();
        assertNotNull(members, "List should not be null");
        assertTrue(members.size() >= 2, "Should have at least 2 members");
    }

    @Test
    public void testListAllMembersEmpty() throws Exception {
        // Test with potentially empty database
        mockMvc.perform(get("/rest/members"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());

        List<Member> members = restService.listAllMembers();
        assertNotNull(members, "List should not be null even when empty");
    }

    @Test
    public void testLookupMemberById() throws Exception {
        // Create and persist a member
        Member member = createTestMember("Charlie", "charlie@rest.com", "3333333333");
        memberRegistration.register(member);
        Long memberId = member.getId();
        assertNotNull(memberId, "Member ID should not be null");

        // Test via REST endpoint
        mockMvc.perform(get("/rest/members/{id}", memberId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(memberId))
            .andExpect(jsonPath("$.name").value("Charlie"))
            .andExpect(jsonPath("$.email").value("charlie@rest.com"));

        // Test via service directly
        ResponseEntity<Member> response = restService.lookupMemberById(memberId);
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status should be OK");
        Member found = response.getBody();
        assertNotNull(found, "Member should be found");
        assertEquals("Charlie", found.getName(), "Name should match");
        assertEquals("charlie@rest.com", found.getEmail(), "Email should match");
    }

    @Test
    public void testLookupMemberByIdNotFound() throws Exception {
        // Test via REST endpoint
        mockMvc.perform(get("/rest/members/{id}", 99999L))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateMemberSuccess() throws Exception {
        // Create a valid member
        Member member = createTestMember("David", "david@rest.com", "4444444444");
        String memberJson = objectMapper.writeValueAsString(member);

        // Test via REST endpoint
        mockMvc.perform(post("/rest/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson))
            .andExpect(status().isOk());

        // Test via service directly
        ResponseEntity<?> response = restService.createMember(member);
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status should be OK");
    }

    @Test
    public void testCreateMemberValidationFailure() throws Exception {
        // Create an invalid member (null name)
        Member member = new Member();
        member.setName(null); // Invalid: @NotNull constraint
        member.setEmail("invalid@rest.com");
        member.setPhoneNumber("5555555555");
        String memberJson = objectMapper.writeValueAsString(member);

        // Test via REST endpoint
        mockMvc.perform(post("/rest/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson))
            .andExpect(status().isBadRequest());

        // Test via service directly
        ResponseEntity<?> response = restService.createMember(member);
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Status should be BAD_REQUEST");
    }

    @Test
    public void testCreateMemberDuplicateEmail() throws Exception {
        // Create and persist first member
        Member member1 = createTestMember("Eve", "eve@rest.com", "6666666666");
        memberRegistration.register(member1);

        // Try to create another member with same email
        Member member2 = createTestMember("Eve2", "eve@rest.com", "7777777777");
        String memberJson = objectMapper.writeValueAsString(member2);

        // Test via REST endpoint
        mockMvc.perform(post("/rest/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson))
            .andExpect(status().isConflict());

        // Test via service directly
        ResponseEntity<?> response = restService.createMember(member2);
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(), "Status should be CONFLICT");
    }

    @Test
    public void testCreateMemberInvalidEmail() throws Exception {
        // Create member with invalid email format
        Member member = createTestMember("Frank", "invalid-email", "8888888888");
        String memberJson = objectMapper.writeValueAsString(member);

        // Test via REST endpoint
        mockMvc.perform(post("/rest/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testEmailAlreadyExists() throws Exception {
        // Create and persist a member
        Member member = createTestMember("Henry", "henry@rest.com", "1010101010");
        memberRegistration.register(member);

        // Test emailAlreadyExists with existing email
        boolean exists = restService.emailAlreadyExists("henry@rest.com");
        assertTrue(exists, "Email should exist");

        // Test emailAlreadyExists with non-existent email
        boolean notExists = restService.emailAlreadyExists("nonexistent@rest.com");
        assertTrue(!notExists, "Email should not exist");
    }

    private Member createTestMember(String name, String email, String phoneNumber) {
        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }
}


