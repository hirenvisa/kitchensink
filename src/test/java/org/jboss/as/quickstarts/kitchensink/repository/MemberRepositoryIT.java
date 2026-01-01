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
package org.jboss.as.quickstarts.kitchensink.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import jakarta.persistence.NoResultException;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring Boot integration tests for MemberRepository
 * Uses @DataJpaTest for repository layer testing with in-memory database
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
@Import({MemberRegistration.class})
public class MemberRepositoryIT {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRegistration memberRegistration;

    @Test
    public void testFindById() throws Exception {
        // Create and persist a member
        Member member = createTestMember("Alice", "alice@test.com", "1234567890");
        member = entityManager.persistAndFlush(member);
        Long memberId = member.getId();
        assertNotNull("Member ID should not be null", memberId);

        // Test findById with valid ID
        Member found = memberRepository.findById(memberId);
        assertNotNull("Member should be found", found);
        assertEquals("Name should match", "Alice", found.getName());
        assertEquals("Email should match", "alice@test.com", found.getEmail());

        // Test findById with non-existent ID
        Member notFound = memberRepository.findById(99999L);
        assertNull("Member should not be found", notFound);
    }

    @Test
    public void testFindByEmail() throws Exception {
        // Create and persist a member
        Member member = createTestMember("Bob", "bob@test.com", "9876543210");
        member = entityManager.persistAndFlush(member);

        // Test findByEmail with valid email
        Member found = memberRepository.findByEmail("bob@test.com");
        assertNotNull("Member should be found", found);
        assertEquals("Name should match", "Bob", found.getName());
        assertEquals("Email should match", "bob@test.com", found.getEmail());

        // Test findByEmail with non-existent email
        try {
            memberRepository.findByEmail("nonexistent@test.com");
            fail("Should have thrown NoResultException");
        } catch (NoResultException e) {
            // Expected exception
        }
    }

    @Test
    public void testFindAllOrderedByName() throws Exception {
        // Create multiple members with different names
        Member member1 = createTestMember("Charlie", "charlie@test.com", "1111111111");
        Member member2 = createTestMember("Alice", "alice2@test.com", "2222222222");
        Member member3 = createTestMember("Bob", "bob2@test.com", "3333333333");

        entityManager.persistAndFlush(member1);
        entityManager.persistAndFlush(member2);
        entityManager.persistAndFlush(member3);

        // Test findAllOrderedByName
        List<Member> members = memberRepository.findAllOrderedByName();
        assertNotNull("List should not be null", members);
        assertTrue("Should have at least 3 members", members.size() >= 3);

        // Verify ordering (Alice, Bob, Charlie)
        boolean foundAlice = false;
        boolean foundBob = false;
        boolean foundCharlie = false;
        int aliceIndex = -1;
        int bobIndex = -1;
        int charlieIndex = -1;

        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if ("Alice".equals(m.getName())) {
                foundAlice = true;
                aliceIndex = i;
            } else if ("Bob".equals(m.getName())) {
                foundBob = true;
                bobIndex = i;
            } else if ("Charlie".equals(m.getName())) {
                foundCharlie = true;
                charlieIndex = i;
            }
        }

        assertTrue("Alice should be found", foundAlice);
        assertTrue("Bob should be found", foundBob);
        assertTrue("Charlie should be found", foundCharlie);

        // Verify alphabetical order
        assertTrue("Alice should come before Bob", aliceIndex < bobIndex);
        assertTrue("Bob should come before Charlie", bobIndex < charlieIndex);
    }

    @Test
    public void testFindAllOrderedByNameEmpty() throws Exception {
        // Test with empty database
        List<Member> members = memberRepository.findAllOrderedByName();
        assertNotNull("List should not be null even when empty", members);
    }

    @Test
    public void testFindAllOrderedByNameWithSpecialCharacters() throws Exception {
        // Test ordering with special characters
        Member member1 = createTestMember("Zebra", "zebra@test.com", "1111111111");
        Member member2 = createTestMember("Álice", "alice3@test.com", "2222222222");
        Member member3 = createTestMember("Bob", "bob3@test.com", "3333333333");

        entityManager.persistAndFlush(member1);
        entityManager.persistAndFlush(member2);
        entityManager.persistAndFlush(member3);

        List<Member> members = memberRepository.findAllOrderedByName();
        assertNotNull("List should not be null", members);
        assertTrue("Should have at least 3 members", members.size() >= 3);
    }

    private Member createTestMember(String name, String email, String phoneNumber) {
        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }
}


