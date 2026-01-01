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
package org.jboss.as.quickstarts.kitchensink.controller;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Spring MVC Controller for member registration with Thymeleaf views
 */
@Controller
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberRegistration memberRegistration;

    public MemberController(MemberRepository memberRepository, MemberRegistration memberRegistration) {
        this.memberRepository = memberRepository;
        this.memberRegistration = memberRegistration;
    }

    /**
     * Display the main page with registration form and member list
     */
    @GetMapping("/")
    public String index(Model model) {
        // Add a new empty member for the form
        if (!model.containsAttribute("newMember")) {
            model.addAttribute("newMember", new Member());
        }
        
        // Add list of all members
        model.addAttribute("members", memberRepository.findAllOrderedByName());
        
        return "index";
    }

    /**
     * Handle member registration form submission
     */
    @PostMapping("/members/register")
    public String register(@Valid @ModelAttribute("newMember") Member newMember,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newMember", bindingResult);
            redirectAttributes.addFlashAttribute("newMember", newMember);
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the form errors.");
            return "redirect:/";
        }

        // Check for duplicate email
        if (memberRepository.findByEmail(newMember.getEmail()) != null) {
            redirectAttributes.addFlashAttribute("newMember", newMember);
            redirectAttributes.addFlashAttribute("errorMessage", "Email address is already registered.");
            return "redirect:/";
        }

        try {
            // Register the new member
            memberRegistration.register(newMember);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Member " + newMember.getName() + " has been successfully registered!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("newMember", newMember);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Registration failed: " + e.getMessage());
            return "redirect:/";
        }

        return "redirect:/";
    }
}
