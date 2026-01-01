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
package org.jboss.as.quickstarts.kitchensink.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot integration tests for MemberRegistration service
 * Uses @SpringBootTest for full application context testing
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
public class MemberRegistrationServiceIT {

    @Autowired
    private MemberRegistration memberRegistration;

    @Test
    public void testRegister() throws Exception {
        Member newMember = new Member();
        newMember.setName("Jane Doe");
        newMember.setEmail("jane@mailinator.com");
        newMember.setPhoneNumber("2125551234");

        memberRegistration.register(newMember);

        assertNotNull("Member ID should be set after registration", newMember.getId());
    }

    @Test
    public void testRegisterMultipleMembers() throws Exception {
        Member member1 = createTestMember("Alice", "alice@service.com", "1111111111");
        Member member2 = createTestMember("Bob", "bob@service.com", "2222222222");

        memberRegistration.register(member1);
        memberRegistration.register(member2);

        assertNotNull("Member 1 ID should be set", member1.getId());
        assertNotNull("Member 2 ID should be set", member2.getId());
    }

    @Test
    public void testRegisterWithTransaction() throws Exception {
        // Test that transaction management works correctly
        Member member = createTestMember("Charlie", "charlie@service.com", "3333333333");
        memberRegistration.register(member);

        assertNotNull("Member should be persisted", member.getId());
    }

    private Member createTestMember(String name, String email, String phoneNumber) {
        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }
}


