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

import java.util.logging.Logger;

import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

// Spring Service with transactional support
@Service
@Transactional
public class MemberRegistration {

    private static final Logger log = Logger.getLogger(MemberRegistration.class.getName());

    private final EntityManager em;
    private final ApplicationEventPublisher eventPublisher;

    public MemberRegistration(EntityManager em, ApplicationEventPublisher eventPublisher) {
        this.em = em;
        this.eventPublisher = eventPublisher;
    }

    public void register(Member member) throws Exception {
        log.info("Registering " + member.getName());
        em.persist(member);
        eventPublisher.publishEvent(member);
    }
}
