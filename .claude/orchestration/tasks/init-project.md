# Task: Initialize Project

## Purpose
Set up a new project with orchestration templates and structure

## Steps

### 1. Create Project Structure
```
Project/
├── project-context.md
├── Docs/
│   ├── Product/
│   ├── Technical/
│   ├── Design/
│   ├── Stories/
│   │   ├── current/
│   │   └── completed/
│   └── Research/
```

### 2. Copy Templates
Copy templates from orchestration directory to project:
- `prd-template.md` → `Docs/Product/`
- `technical-spec-template.md` → `Docs/Technical/`
- `design-spec-template.md` → `Docs/Design/`
- `story-template.md` → `Docs/Stories/`
- `project-context-template.md` → `./project-context.md`

### 3. Initialize Project Context
Create initial project-context.md with:
```markdown
# Project Context Document

## Document Metadata
- **Project Name**: [Project Name]
- **Created Date**: [Today]
- **Current Phase**: Discovery
- **Overall Status**: 🟢 On Track

## 1. Project Overview
### Project Summary
[One paragraph describing the project]

### Key Objectives
1. [Primary objective]
2. [Secondary objective]

## 2. Active Agents & Responsibilities
| Agent | Role | Current Task | Status |
|-------|------|--------------|--------|
| workflow-orchestrator | Process coordinator | Initializing | ✅ Active |
| elon | Product vision | Pending | ⏸️ Waiting |
| market-researcher | Market validation | Pending | ⏸️ Waiting |
| tech-lead | Architecture | Pending | ⏸️ Waiting |
| designer | UI/UX | Pending | ⏸️ Waiting |
| ios-developer | Implementation | Pending | ⏸️ Waiting |

## 3. Current Sprint/Phase
- **Current Phase**: Project Initialization
- **Next Step**: Product Vision (elon)

## 4. Completed Artifacts
| Document | Status | Location |
|----------|--------|----------|
| Project Structure | ✅ Complete | ./ |
| Templates | ✅ Copied | Docs/ |

## 5. Next Actions
1. Activate elon agent for product vision
2. Create initial PRD
3. Begin market validation
```

### 4. Set Permissions
Ensure all directories and files are accessible:
- Read/Write for current user
- Templates should be editable

### 5. Verification Checklist
- [ ] All directories created
- [ ] All templates copied
- [ ] project-context.md initialized
- [ ] Core configuration accessible
- [ ] Ready for first agent activation

## Success Criteria
- Project structure matches orchestration standards
- All templates available in correct locations
- Project context tracks initialization
- Ready for workflow execution

## Error Handling
If directory exists: Use existing structure
If template missing: Report which template
If permission denied: Request user intervention

## Output Message (Korean)
```
프로젝트 초기화가 완료되었습니다.

✅ 생성된 구조:
- 프로젝트 디렉토리
- 문서 템플릿
- 컨텍스트 파일

📋 다음 단계:
elon 에이전트를 활성화하여 제품 비전을 수립하세요.
```