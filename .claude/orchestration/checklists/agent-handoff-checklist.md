# Agent Handoff Checklist

## Purpose
이 체크리스트는 에이전트 간 작업 전달 시 정보 손실을 방지하고 원활한 협업을 보장합니다.

---

## 📤 Handoff FROM: elon → market-researcher

### Pre-Handoff Requirements
- [ ] PRD 초안 작성 완료
- [ ] 제품 비전 섹션 완성
- [ ] 핵심 가치 제안 정의
- [ ] 타겟 사용자 명확화
- [ ] 성공 지표 초안 작성

### Documents to Transfer
- [ ] PRD v0.x (Executive Summary, Product Definition 완성)
- [ ] 사용자 페르소나 문서
- [ ] 경쟁사 초기 분석 (있는 경우)
- [ ] 프로젝트 컨텍스트 업데이트

### Validation Questions for market-researcher
1. 제안된 제품이 실제 시장 수요를 충족하는가?
2. 타겟 시장 규모는 충분한가?
3. 경쟁 우위를 확보할 수 있는가?
4. 제안된 가격 모델이 현실적인가?
5. 규제나 법적 제약사항이 있는가?

### Handoff Message Template
```markdown
## Handoff: elon → market-researcher

**PRD Status**: Draft v0.1 complete
**Key Assumptions to Validate**:
1. [Assumption 1]
2. [Assumption 2]

**Priority Research Areas**:
- Market size validation
- Competitor analysis
- User willingness to pay

**Timeline**: Please complete by [Date]
```

---

## 📤 Handoff FROM: market-researcher → tech-lead

### Pre-Handoff Requirements
- [ ] 시장 분석 완료
- [ ] 경쟁사 분석 완료
- [ ] 사용자 조사 결과 문서화
- [ ] PRD 검증 및 업데이트
- [ ] 기술적 제약사항 식별

### Documents to Transfer
- [ ] Market Analysis Report
- [ ] Updated PRD (with market validation)
- [ ] Competitive Analysis
- [ ] User Research Findings
- [ ] Technical Constraints (identified)

### Key Information for tech-lead
- [ ] 예상 사용자 규모
- [ ] 성능 요구사항
- [ ] 확장성 요구사항
- [ ] 보안 요구사항
- [ ] 통합 필요 시스템

### Handoff Message Template
```markdown
## Handoff: market-researcher → tech-lead

**Market Validation**: ✅ Complete
**Expected User Scale**: [Number]
**Critical Technical Requirements**:
1. [Requirement 1]
2. [Requirement 2]

**Integration Points**:
- [System 1]
- [System 2]

**Architecture Considerations**:
- [Consideration 1]
- [Consideration 2]
```

---

## 📤 Handoff FROM: tech-lead → designer

### Pre-Handoff Requirements
- [ ] 기술 아키텍처 설계 완료
- [ ] API 스펙 정의
- [ ] 기술적 제약사항 문서화
- [ ] 플랫폼 요구사항 명시
- [ ] 성능 목표 설정

### Documents to Transfer
- [ ] Technical Specification
- [ ] API Documentation
- [ ] Platform Constraints
- [ ] Performance Requirements
- [ ] Security Guidelines

### Design Constraints to Communicate
- [ ] 플랫폼별 제한사항
- [ ] 성능 관련 디자인 제약
- [ ] 데이터 로딩 패턴
- [ ] 오프라인 모드 요구사항
- [ ] 접근성 요구사항

### Handoff Message Template
```markdown
## Handoff: tech-lead → designer

**Architecture**: ✅ Defined
**Platform**: iOS 15+
**Key Technical Constraints**:
1. [Constraint 1]
2. [Constraint 2]

**Performance Targets**:
- Load time: < 2s
- Animation: 60fps

**API Endpoints Available**:
- [Endpoint list]
```

---

## 📤 Handoff FROM: designer → android-developer

### Pre-Handoff Requirements
- [ ] 모든 화면 디자인 완료
- [ ] 디자인 시스템 정의
- [ ] 인터랙션 패턴 문서화
- [ ] 에셋 export 완료
- [ ] 디자인 QA 완료

### Documents to Transfer
- [ ] Design Specification
- [ ] Figma/Sketch Files (with dev mode access)
- [ ] Design System Documentation
- [ ] Exported Assets (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- [ ] Animation Specifications

### Implementation Guidelines
- [ ] 컬러 토큰 매핑
- [ ] 타이포그래피 스케일
- [ ] 스페이싱 시스템
- [ ] 컴포넌트 라이브러리
- [ ] 애니메이션 타이밍

### Handoff Message Template
```markdown
## Handoff: designer → android-developer

**Design Status**: ✅ Complete
**Figma File**: [Link]
**Material Design System**: [Link]

**Implementation Priority**:
1. [Screen 1]
2. [Screen 2]

**Special Considerations**:
- [Custom animation on Screen X]
- [Complex gesture on Screen Y]

**Assets Location**: /assets/export/
```

---

## 📤 Handoff FROM: android-developer → QA/Testing

### Pre-Handoff Requirements
- [ ] 모든 기능 구현 완료
- [ ] 유닛 테스트 작성 (>80% coverage)
- [ ] 코드 리뷰 완료
- [ ] 문서화 업데이트
- [ ] 빌드 성공

### Documents to Transfer
- [ ] Updated Story Document (with implementation notes)
- [ ] Test Coverage Report
- [ ] Known Issues List
- [ ] API Integration Status
- [ ] Performance Metrics

### Testing Information
- [ ] 테스트 환경 설정
- [ ] 테스트 데이터 준비
- [ ] 테스트 시나리오
- [ ] 엣지 케이스 목록
- [ ] 디바이스 호환성

### Handoff Message Template
```markdown
## Handoff: android-developer → QA

**Build Version**: 1.0.0-beta.1
**Play Console Internal Testing Link**: [Link]
**Test Environment**: Staging

**Implemented Features**:
- ✅ [Feature 1]
- ✅ [Feature 2]

**Known Issues**:
1. [Issue 1] - Workaround: [Solution]

**Test Priority**:
1. [Critical path 1]
2. [Critical path 2]
```

---

## 🔄 Generic Handoff Checklist

### Before Handoff
- [ ] 현재 작업 100% 완료 확인
- [ ] 모든 문서 최신 버전으로 업데이트
- [ ] 프로젝트 컨텍스트 업데이트
- [ ] 블로커 및 이슈 문서화
- [ ] 다음 단계 명확히 정의

### During Handoff
- [ ] 명확한 handoff 메시지 작성
- [ ] 모든 필요 문서 첨부
- [ ] 우선순위 명시
- [ ] 타임라인 공유
- [ ] 질문사항 목록 제공

### After Handoff
- [ ] 수신 확인 대기
- [ ] 초기 질문 대응
- [ ] 필요시 추가 정보 제공
- [ ] 전환 기간 동안 지원
- [ ] 컨텍스트 문서 최종 업데이트

---

## 🚨 Emergency Handoff Protocol

### When to Use
- 에이전트 타임아웃 (24시간 무응답)
- 긴급 변경사항 발생
- 블로커로 인한 재배치
- 우선순위 변경

### Quick Handoff Steps
1. **현재 상태 스냅샷**
   ```markdown
   ## Emergency Handoff
   **From**: [Agent]
   **To**: [Agent]
   **Reason**: [Why emergency]
   **Current Status**: [% complete]
   **Next Actions**: [List]
   **Blockers**: [List]
   ```

2. **최소 필요 문서**
   - Current work file
   - Project context (latest)
   - Blocker details

3. **즉시 조치 사항**
   - [ ] 새 에이전트 활성화
   - [ ] 컨텍스트 빠른 브리핑
   - [ ] 긴급 작업 우선 처리

---

## 📊 Handoff Quality Metrics

### Success Criteria
- [ ] 정보 손실 없음
- [ ] 재작업 불필요
- [ ] 명확한 다음 단계
- [ ] 모든 질문 해결
- [ ] 타임라인 준수

### Quality Score Calculation
```
Quality Score = (
  Document Completeness (30%) +
  Context Clarity (25%) +
  No Rework Needed (25%) +
  Timeline Met (10%) +
  Smooth Transition (10%)
) / 100
```

### Target Metrics
- **Handoff Success Rate**: > 95%
- **Average Handoff Time**: < 2 hours
- **Rework Rate**: < 5%
- **Context Loss**: < 2%

---

## 🔧 Troubleshooting Guide

### Common Issues
| Issue | Symptoms | Solution |
|-------|----------|----------|
| Incomplete Context | Missing information | Review checklist, request details |
| Version Conflict | Document mismatch | Sync to latest, merge changes |
| Unclear Requirements | Ambiguous next steps | Clarification meeting, update docs |
| Missing Documents | 404 errors | Check paths, restore from backup |
| Communication Gap | No response | Escalate, use emergency protocol |

---

## 📝 Notes

### Best Practices
1. **Over-communicate**: 정보가 부족한 것보다 많은 것이 낫다
2. **Document Everything**: 모든 결정과 변경사항 기록
3. **Verify Receipt**: 항상 수신 확인
4. **Stay Available**: 전환 기간 동안 대기
5. **Learn & Improve**: 각 handoff에서 배운 점 기록

### Continuous Improvement
- 매 스프린트 후 체크리스트 검토
- 실패한 handoff 분석
- 프로세스 개선사항 제안
- 템플릿 정기 업데이트

---

<!-- 
USAGE INSTRUCTIONS:
1. Select appropriate handoff section
2. Complete all checkboxes
3. Fill in template with actual data
4. Verify all documents attached
5. Send handoff message
6. Monitor for response
7. Update project context
-->