# Product Requirements Document (PRD)

## Document Metadata
- **Document Version**: 1.0.0
- **Created Date**: [YYYY-MM-DD]
- **Last Modified**: [YYYY-MM-DD]
- **Status**: [Draft | In Review | Approved | Archived]
- **Author**: [Agent: elon/market-researcher/human]
- **Reviewers**: []

---

## 1. Executive Summary
<!-- elon 전용 섹션: 제품 비전과 핵심 가치 제안 -->
### Product Vision
[One-sentence product vision that captures the essence]

### Key Value Propositions
1. [Primary value prop]
2. [Secondary value prop]
3. [Tertiary value prop]

### Target Outcomes
- **Business Impact**: [Expected business outcome]
- **User Impact**: [Expected user benefit]
- **Technical Impact**: [Expected technical improvement]

---

## 2. Market Analysis
<!-- market-researcher 전용 섹션: 시장 조사 및 검증 -->
### Market Size & Opportunity
- **TAM (Total Addressable Market)**: $[amount]
- **SAM (Serviceable Addressable Market)**: $[amount]
- **SOM (Serviceable Obtainable Market)**: $[amount]

### Competitive Landscape
| Competitor | Strengths | Weaknesses | Market Share | Our Differentiation |
|------------|-----------|------------|--------------|-------------------|
| [Name] | [List] | [List] | [%] | [How we're different] |

### User Research Insights
- **Research Method**: [Surveys/Interviews/Analytics]
- **Sample Size**: [N=]
- **Key Findings**:
  1. [Finding 1]
  2. [Finding 2]
  3. [Finding 3]

### Market Validation
- **Problem Validation Score**: [0-10]
- **Solution Fit Score**: [0-10]
- **Willingness to Pay**: [Evidence]

---

## 3. Product Definition
<!-- 공통 섹션: 제품 정의 및 요구사항 -->

### Problem Statement
**User Problem**: [Clear description of the problem we're solving]
**Current Solutions**: [How users currently solve this]
**Why Current Solutions Fail**: [Gap analysis]

### Solution Overview
[High-level description of the proposed solution]

### User Personas
#### Primary Persona: [Name]
- **Demographics**: [Age, profession, etc.]
- **Goals**: [What they want to achieve]
- **Pain Points**: [Current frustrations]
- **Jobs to be Done**: [Tasks they need to complete]

#### Secondary Persona: [Name]
[Similar structure]

---

## 4. Functional Requirements
<!-- 개발 가능한 명확한 요구사항 -->

### Core Features (MVP)
| Feature ID | Feature Name | Description | User Story | Priority |
|------------|--------------|-------------|------------|----------|
| F001 | [Name] | [What it does] | As a [user], I want to [action] so that [benefit] | P0 |
| F002 | [Name] | [What it does] | As a [user], I want to [action] so that [benefit] | P0 |

### Future Features (Post-MVP)
| Feature ID | Feature Name | Description | Estimated Phase |
|------------|--------------|-------------|-----------------|
| F101 | [Name] | [What it does] | Phase 2 |

### User Flows
```
[Start] → [Step 1] → [Decision Point] → [Step 2] → [End]
                          ↓
                    [Alternative Path]
```

---

## 5. Non-Functional Requirements
<!-- 시스템 품질 요구사항 -->

### Performance Requirements
- **Response Time**: < [X]ms for [operation]
- **Throughput**: [X] requests/second
- **Concurrent Users**: Support [X] concurrent users

### Security Requirements
- **Authentication**: [Method]
- **Authorization**: [Role-based/Attribute-based]
- **Data Protection**: [Encryption standards]

### Usability Requirements
- **Accessibility**: WCAG 2.1 Level [A/AA/AAA]
- **Platform Support**: [iOS 15+, Android 12+, Web]
- **Localization**: [Languages]

---

## 6. Success Metrics
<!-- 측정 가능한 성공 지표 -->

### Business Metrics
| Metric | Current Baseline | Target | Timeline | Measurement Method |
|--------|-----------------|--------|----------|-------------------|
| [Revenue] | $[X] | $[Y] | [Q] | [How to measure] |
| [User Growth] | [X] | [Y] | [Q] | [How to measure] |

### Product Metrics
| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Daily Active Users | [X] | [Analytics tool] |
| Feature Adoption Rate | [X]% | [Feature analytics] |
| User Retention (D7/D30) | [X]%/[Y]% | [Cohort analysis] |

### Quality Metrics
| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Crash Rate | < [X]% | [Monitoring tool] |
| User Satisfaction (NPS) | > [X] | [Survey] |
| Support Ticket Volume | < [X]/week | [Support system] |

---

## 7. Constraints & Assumptions

### Constraints
- **Budget**: $[amount]
- **Timeline**: [Launch date]
- **Resources**: [Team size and composition]
- **Technical**: [Platform limitations]
- **Legal/Compliance**: [Regulations to follow]

### Assumptions
1. [Assumption about users]
2. [Assumption about market]
3. [Assumption about technology]

### Dependencies
- **Internal**: [Other teams/systems]
- **External**: [Third-party services/APIs]

---

## 8. Risks & Mitigation

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|--------|
| [Risk description] | High/Med/Low | High/Med/Low | [How to address] | [Who] |

---

## 9. Timeline & Milestones

### Development Phases
| Phase | Description | Duration | Deliverables |
|-------|-------------|----------|--------------|
| Discovery | Research & Planning | [X weeks] | [List] |
| Design | UX/UI Design | [X weeks] | [List] |
| Development | Implementation | [X weeks] | [List] |
| Testing | QA & UAT | [X weeks] | [List] |
| Launch | Deployment & Monitoring | [X weeks] | [List] |

### Key Milestones
- [ ] **[Date]**: [Milestone description]
- [ ] **[Date]**: [Milestone description]
- [ ] **[Date]**: [Milestone description]

---

## 10. Appendices

### A. Glossary
| Term | Definition |
|------|------------|
| [Term] | [Clear definition] |

### B. References
- [Document/Link 1]
- [Document/Link 2]

### C. Changelog
| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0.0 | [Date] | Initial draft | [Name] |

---

## Agent Handoff Notes
<!-- 에이전트 간 전달 사항 -->
### For tech-lead
- [ ] Review technical feasibility of features F001-F005
- [ ] Propose architecture for scalability requirements
- [ ] Identify potential technical risks

### For designer
- [ ] Create wireframes for core user flows
- [ ] Design component library based on brand guidelines
- [ ] Ensure accessibility requirements are met

### For ios-developer
- [ ] Implement features according to priority
- [ ] Follow platform-specific guidelines
- [ ] Integrate with backend APIs as specified

---

<!-- 
USAGE INSTRUCTIONS:
1. elon agent: Fill sections 1 (Executive Summary) and 3 (Product Definition)
2. market-researcher agent: Complete section 2 (Market Analysis) and validate section 1
3. Both agents can collaborate on sections 4-9
4. Use [brackets] for placeholder content to be filled
5. Maintain version control for all changes
6. Get approval before moving to next phase
-->