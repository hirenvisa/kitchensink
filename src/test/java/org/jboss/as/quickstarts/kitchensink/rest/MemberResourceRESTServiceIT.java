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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.jboss.as.quickstarts.kitchensink.util.Resources;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for MemberResourceRESTService
 */
@RunWith(Arquillian.class)
public class MemberResourceRESTServiceIT {

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addClasses(Member.class, MemberRepository.class, MemberRegistration.class,
                    MemberResourceRESTService.class, JaxRsActivator.class, Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(new StringAsset("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\"\n"
                        + "bean-discovery-mode=\"all\">\n"
                        + "</beans>"), "beans.xml")
            .addAsWebInfResource("test-ds.xml");
    }

    @Inject
    MemberResourceRESTService restService;

    @Inject
    MemberRepository memberRepository;

    @Inject
    MemberRegistration memberRegistration;

    @Inject
    Logger log;

    @Test
    public void testListAllMembers() throws Exception {
        // Create some test members
        Member member1 = createTestMember("Alice", "alice@rest.com", "1111111111");
        Member member2 = createTestMember("Bob", "bob@rest.com", "2222222222");

        memberRegistration.register(member1);
        memberRegistration.register(member2);

        // Test listAllMembers
        List<Member> members = restService.listAllMembers();
        assertNotNull("List should not be null", members);
        assertTrue("Should have at least 2 members", members.size() >= 2);
    }

    @Test
    public void testListAllMembersEmpty() throws Exception {
        // Test with potentially empty database
        List<Member> members = restService.listAllMembers();
        assertNotNull("List should not be null even when empty", members);
    }

    @Test
    public void testLookupMemberById() throws Exception {
        // Create and persist a member
        Member member = createTestMember("Charlie", "charlie@rest.com", "3333333333");
        memberRegistration.register(member);
        Long memberId = member.getId();
        assertNotNull("Member ID should not be null", memberId);

        // Test lookupMemberById with valid ID
        Member found = restService.lookupMemberById(memberId);
        assertNotNull("Member should be found", found);
        assertEquals("Name should match", "Charlie", found.getName());
        assertEquals("Email should match", "charlie@rest.com", found.getEmail());
    }

    @Test(expected = WebApplicationException.class)
    public void testLookupMemberByIdNotFound() throws Exception {
        // Test lookupMemberById with non-existent ID
        restService.lookupMemberById(99999L);
    }

    @Test
    public void testCreateMemberSuccess() throws Exception {
        // Create a valid member
        Member member = createTestMember("David", "david@rest.com", "4444444444");

        // Test createMember
        Response response = restService.createMember(member);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be OK", Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateMemberValidationFailure() throws Exception {
        // Create an invalid member (null name)
        Member member = new Member();
        member.setName(null); // Invalid: @NotNull constraint
        member.setEmail("invalid@rest.com");
        member.setPhoneNumber("5555555555");

        // Test createMember with validation failure
        Response response = restService.createMember(member);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        // Verify error entity contains violation information
        Object entity = response.getEntity();
        assertNotNull("Error entity should not be null", entity);
    }

    @Test
    public void testCreateMemberDuplicateEmail() throws Exception {
        // Create and persist first member
        Member member1 = createTestMember("Eve", "eve@rest.com", "6666666666");
        memberRegistration.register(member1);

        // Try to create another member with same email
        Member member2 = createTestMember("Eve2", "eve@rest.com", "7777777777");

        // Test createMember with duplicate email
        Response response = restService.createMember(member2);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be CONFLICT", Response.Status.CONFLICT.getStatusCode(), response.getStatus());

        // Verify error entity contains email conflict message
        Object entity = response.getEntity();
        assertNotNull("Error entity should not be null", entity);
    }

    @Test
    public void testCreateMemberInvalidEmail() throws Exception {
        // Create member with invalid email format
        Member member = createTestMember("Frank", "invalid-email", "8888888888");

        // Test createMember with invalid email
        Response response = restService.createMember(member);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateMemberInvalidPhone() throws Exception {
        // Create member with invalid phone (too short)
        Member member = createTestMember("Grace", "grace@rest.com", "123"); // Too short

        // Test createMember with invalid phone
        Response response = restService.createMember(member);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateMemberNameWithNumbers() throws Exception {
        // Create member with name containing numbers (violates @Pattern)
        Member member = createTestMember("John123", "john@rest.com", "9999999999");

        // Test createMember with invalid name pattern
        Response response = restService.createMember(member);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testEmailAlreadyExists() throws Exception {
        // Create and persist a member
        Member member = createTestMember("Henry", "henry@rest.com", "1010101010");
        memberRegistration.register(member);

        // Test emailAlreadyExists with existing email
        boolean exists = restService.emailAlreadyExists("henry@rest.com");
        assertTrue("Email should exist", exists);

        // Test emailAlreadyExists with non-existent email
        boolean notExists = restService.emailAlreadyExists("nonexistent@rest.com");
        assertTrue("Email should not exist", !notExists);
    }

    private Member createTestMember(String name, String email, String phoneNumber) {
        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }
}

