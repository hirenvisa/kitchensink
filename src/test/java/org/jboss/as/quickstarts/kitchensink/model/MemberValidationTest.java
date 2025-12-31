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
package org.jboss.as.quickstarts.kitchensink.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for Member bean validation constraints
 */
public class MemberValidationTest {

    private static Validator validator;

    @BeforeClass
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidMember() {
        Member member = createValidMember();
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("Valid member should have no violations", violations.isEmpty());
    }

    @Test
    public void testNameNotNull() {
        Member member = createValidMember();
        member.setName(null);
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Null name should have violations", violations.isEmpty());
        assertTrue("Should have @NotNull violation for name",
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    public void testNameSizeMin() {
        Member member = createValidMember();
        member.setName(""); // Empty string violates @Size(min=1)
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Empty name should have violations", violations.isEmpty());
    }

    @Test
    public void testNameSizeMax() {
        Member member = createValidMember();
        member.setName("A".repeat(26)); // 26 characters violates @Size(max=25)
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Name too long should have violations", violations.isEmpty());
    }

    @Test
    public void testNamePatternNoNumbers() {
        Member member = createValidMember();
        member.setName("John123"); // Contains numbers violates @Pattern
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Name with numbers should have violations", violations.isEmpty());
        assertTrue("Should have @Pattern violation for name",
            violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("name") &&
                v.getMessage().contains("Must not contain numbers")));
    }

    @Test
    public void testNamePatternValid() {
        Member member = createValidMember();
        member.setName("John Doe"); // Valid name without numbers
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("Valid name should have no violations", violations.isEmpty());
    }

    @Test
    public void testEmailNotNull() {
        Member member = createValidMember();
        member.setEmail(null);
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Null email should have violations", violations.isEmpty());
    }

    @Test
    public void testEmailNotEmpty() {
        Member member = createValidMember();
        member.setEmail(""); // Empty string violates @NotEmpty
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Empty email should have violations", violations.isEmpty());
    }

    @Test
    public void testEmailFormat() {
        Member member = createValidMember();
        member.setEmail("invalid-email"); // Invalid email format
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Invalid email format should have violations", violations.isEmpty());
        assertTrue("Should have @Email violation",
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    public void testEmailValid() {
        Member member = createValidMember();
        member.setEmail("valid@example.com"); // Valid email
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("Valid email should have no violations", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberNotNull() {
        Member member = createValidMember();
        member.setPhoneNumber(null);
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Null phone number should have violations", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberSizeMin() {
        Member member = createValidMember();
        member.setPhoneNumber("123456789"); // 9 characters violates @Size(min=10)
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Phone number too short should have violations", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberSizeMax() {
        Member member = createValidMember();
        member.setPhoneNumber("1234567890123"); // 13 characters violates @Size(max=12)
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Phone number too long should have violations", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberDigits() {
        Member member = createValidMember();
        member.setPhoneNumber("123456789a"); // Contains non-digit violates @Digits
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Phone number with non-digits should have violations", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberValid() {
        Member member = createValidMember();
        member.setPhoneNumber("1234567890"); // Valid 10-digit phone
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("Valid phone number should have no violations", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberValid12Digits() {
        Member member = createValidMember();
        member.setPhoneNumber("123456789012"); // Valid 12-digit phone
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("Valid 12-digit phone number should have no violations", violations.isEmpty());
    }

    @Test
    public void testMultipleViolations() {
        Member member = new Member();
        // Set all fields to invalid values
        member.setName(null);
        member.setEmail("invalid");
        member.setPhoneNumber("123");

        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertFalse("Should have multiple violations", violations.isEmpty());
        assertTrue("Should have at least 3 violations", violations.size() >= 3);
    }

    @Test
    public void testNameBoundaryConditions() {
        Member member = createValidMember();

        // Test minimum valid size (1 character)
        member.setName("A");
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("1-character name should be valid", violations.isEmpty());

        // Test maximum valid size (25 characters)
        member.setName("A".repeat(25));
        violations = validator.validate(member);
        assertTrue("25-character name should be valid", violations.isEmpty());
    }

    @Test
    public void testPhoneNumberBoundaryConditions() {
        Member member = createValidMember();

        // Test minimum valid size (10 digits)
        member.setPhoneNumber("1234567890");
        Set<ConstraintViolation<Member>> violations = validator.validate(member);
        assertTrue("10-digit phone should be valid", violations.isEmpty());
        // Test maximum valid size (12 digits)
        member.setPhoneNumber("123456789012");
        violations = validator.validate(member);
        assertTrue("12-digit phone should be valid", violations.isEmpty());
    }

    private Member createValidMember() {
        Member member = new Member();
        member.setName("John Doe");
        member.setEmail("john@example.com");
        member.setPhoneNumber("1234567890");
        return member;
    }
}

