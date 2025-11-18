#!/usr/bin/env python3
"""
스크린샷 HTML 파일들의 스타일을 미니멀 화이트 베이스로 일괄 업데이트
"""
import os
import re
from pathlib import Path

# 언어 디렉토리 목록
languages = [
    'en', 'ja', 'zh-cn', 'zh-tw', 'vi', 'th', 'in', 'hi',
    'es', 'fr', 'de', 'pt', 'ru', 'it', 'ar', 'tr'
]

# 스크린샷 파일 목록
screenshots = [
    '01-problem.html',
    '02-solution.html',
    '03-feature.html',
    '04-result.html'
]

def update_01_problem(file_path):
    """01-problem.html 스타일 업데이트"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # body background 변경
    content = re.sub(
        r'background:\s*#E5E7EB;',
        'background: #F5F5F5;',
        content
    )

    # phone-frame background 변경 (그라데이션 제거)
    content = re.sub(
        r'background:\s*linear-gradient\(135deg,\s*#1A1A1A\s*0%,\s*#2D2D2D\s*100%\);',
        'background: #FFFFFF;',
        content
    )

    # emoji 스타일 업데이트 (애니메이션 제거, 크기 축소)
    content = re.sub(
        r'\.emoji\s*\{[^}]*\}',
        '''.emoji {
            font-size: 240px;
            margin-bottom: 120px;
        }''',
        content,
        flags=re.DOTALL
    )

    # float 애니메이션 제거
    content = re.sub(
        r'@keyframes\s+float\s*\{[^}]*\}',
        '',
        content,
        flags=re.DOTALL
    )

    # main-message 업데이트
    content = re.sub(
        r'font-size:\s*108px;',
        'font-size: 96px;',
        content
    )
    content = re.sub(
        r'font-weight:\s*900;',
        'font-weight: 800;',
        content
    )
    content = re.sub(
        r'color:\s*#FFFFFF;',
        'color: #1A1A1A;',
        content
    )
    content = re.sub(
        r'line-height:\s*1\.3;',
        'line-height: 1.2;',
        content
    )
    content = re.sub(
        r'margin-bottom:\s*72px;',
        'margin-bottom: 60px;',
        content
    )

    # highlight 스타일 단순화
    content = re.sub(
        r'\.highlight\s*\{[^}]*\}',
        '''.highlight {
            color: #EF4444;
        }''',
        content,
        flags=re.DOTALL
    )

    # sub-message 업데이트
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*font-size:\s*)54px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*color:\s*)#D1D5DB',
        r'\g<1>#6B7280',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*line-height:\s*)1\.6',
        r'\g<1>1.5',
        content
    )

    # stat-number 색상 변경
    content = re.sub(
        r'(\.stat-number\s*\{[^}]*font-size:\s*)144px',
        r'\g<1>120px',
        content
    )
    content = re.sub(
        r'(\.stat-number\s*\{[^}]*color:\s*)#8B5CF6',
        r'\g<1>#EF4444',
        content
    )
    content = re.sub(
        r'(\.stat-number\s*\{[^}]*font-weight:\s*)900',
        r'\g<1>800',
        content
    )

    # stat-label 업데이트
    content = re.sub(
        r'(\.stat-label\s*\{[^}]*font-size:\s*)42px',
        r'\g<1>40px',
        content
    )
    content = re.sub(
        r'(\.stat-label\s*\{[^}]*font-weight:\s*)600',
        r'\g<1>500',
        content
    )

    # brand 업데이트
    content = re.sub(
        r'(\.brand\s*\{[^}]*font-size:\s*)60px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.brand\s*\{[^}]*color:\s*)rgba\(255,\s*255,\s*255,\s*0\.3\)',
        r'\g<1>#E5E7EB',
        content
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def update_02_solution(file_path):
    """02-solution.html 스타일 업데이트"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # body background 변경
    content = re.sub(
        r'background:\s*#E5E7EB;',
        'background: #F5F5F5;',
        content
    )

    # phone-frame background 변경 (그라데이션 제거)
    content = re.sub(
        r'background:\s*linear-gradient\(135deg,\s*#8B5CF6\s*0%,\s*#7C3AED\s*100%\);',
        'background: #FFFFFF;',
        content
    )

    # timer-circle 업데이트
    content = re.sub(
        r'\.timer-circle\s*\{[^}]*\}',
        '''.timer-circle {
            width: 600px;
            height: 600px;
            border-radius: 50%;
            background: #F9FAFB;
            border: 8px solid #8B5CF6;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            margin-bottom: 180px;
        }''',
        content,
        flags=re.DOTALL
    )

    # timer-number 업데이트
    content = re.sub(
        r'(\.timer-number\s*\{[^}]*font-weight:\s*)900',
        r'\g<1>800',
        content
    )
    content = re.sub(
        r'(\.timer-number\s*\{[^}]*color:\s*)#FFFFFF',
        r'\g<1>#8B5CF6',
        content
    )

    # timer-label 업데이트
    content = re.sub(
        r'(\.timer-label\s*\{[^}]*font-weight:\s*)600',
        r'\g<1>500',
        content
    )
    content = re.sub(
        r'(\.timer-label\s*\{[^}]*color:\s*)rgba\(255,\s*255,\s*255,\s*0\.9\)',
        r'\g<1>#6B7280',
        content
    )

    # main-message 업데이트
    content = re.sub(
        r'(\.main-message\s*\{[^}]*font-size:\s*)108px',
        r'\g<1>96px',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*font-weight:\s*)900',
        r'\g<1>800',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*color:\s*)#FFFFFF',
        r'\g<1>#1A1A1A',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*line-height:\s*)1\.3',
        r'\g<1>1.2',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*margin-bottom:\s*)72px',
        r'\g<1>60px',
        content
    )

    # sub-message 업데이트
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*font-size:\s*)54px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*color:\s*)rgba\(255,\s*255,\s*255,\s*0\.8\)',
        r'\g<1>#6B7280',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*line-height:\s*)1\.6',
        r'\g<1>1.5',
        content
    )

    # brand 업데이트
    content = re.sub(
        r'(\.brand\s*\{[^}]*font-size:\s*)60px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.brand\s*\{[^}]*color:\s*)rgba\(255,\s*255,\s*255,\s*0\.3\)',
        r'\g<1>#E5E7EB',
        content
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def update_03_feature(file_path):
    """03-feature.html 스타일 업데이트"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # body background 변경
    content = re.sub(
        r'background:\s*#E5E7EB;',
        'background: #F5F5F5;',
        content
    )

    # emoji 크기 조정
    content = re.sub(
        r'(\.emoji\s*\{[^}]*font-size:\s*)360px',
        r'\g<1>240px',
        content
    )

    # main-message 업데이트
    content = re.sub(
        r'(\.main-message\s*\{[^}]*font-size:\s*)108px',
        r'\g<1>96px',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*font-weight:\s*)900',
        r'\g<1>800',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*line-height:\s*)1\.3',
        r'\g<1>1.2',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*margin-bottom:\s*)72px',
        r'\g<1>60px',
        content
    )

    # sub-message 업데이트
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*font-size:\s*)54px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*line-height:\s*)1\.6',
        r'\g<1>1.5',
        content
    )

    # brand 업데이트
    content = re.sub(
        r'(\.brand\s*\{[^}]*font-size:\s*)60px',
        r'\g<1>48px',
        content
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def update_04_result(file_path):
    """04-result.html 스타일 업데이트"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # body background 변경
    content = re.sub(
        r'background:\s*#E5E7EB;',
        'background: #F5F5F5;',
        content
    )

    # phone-frame background 변경 (그라데이션 제거)
    content = re.sub(
        r'background:\s*linear-gradient\(135deg,\s*#10B981\s*0%,\s*#059669\s*100%\);',
        'background: #FFFFFF;',
        content
    )

    # success-icon 업데이트 (애니메이션 제거, 크기 축소)
    content = re.sub(
        r'\.success-icon\s*\{[^}]*\}',
        '''.success-icon {
            font-size: 240px;
            margin-bottom: 120px;
        }''',
        content,
        flags=re.DOTALL
    )

    # bounce 애니메이션 제거
    content = re.sub(
        r'@keyframes\s+bounce\s*\{[^}]*\}',
        '',
        content,
        flags=re.DOTALL
    )

    # main-message 업데이트
    content = re.sub(
        r'(\.main-message\s*\{[^}]*font-size:\s*)108px',
        r'\g<1>96px',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*font-weight:\s*)900',
        r'\g<1>800',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*color:\s*)#FFFFFF',
        r'\g<1>#1A1A1A',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*line-height:\s*)1\.3',
        r'\g<1>1.2',
        content
    )
    content = re.sub(
        r'(\.main-message\s*\{[^}]*margin-bottom:\s*)72px',
        r'\g<1>60px',
        content
    )

    # highlight 업데이트
    content = re.sub(
        r'\.highlight\s*\{[^}]*\}',
        '''.highlight {
            font-size: 144px;
            display: block;
            margin: 48px 0;
            color: #10B981;
        }''',
        content,
        flags=re.DOTALL
    )

    # sub-message 업데이트
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*font-size:\s*)54px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*color:\s*)rgba\(255,\s*255,\s*255,\s*0\.9\)',
        r'\g<1>#6B7280',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*line-height:\s*)1\.6',
        r'\g<1>1.5',
        content
    )

    # brand 업데이트
    content = re.sub(
        r'(\.brand\s*\{[^}]*font-size:\s*)60px',
        r'\g<1>48px',
        content
    )
    content = re.sub(
        r'(\.brand\s*\{[^}]*color:\s*)rgba\(255,\s*255,\s*255,\s*0\.3\)',
        r'\g<1>#E5E7EB',
        content
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    base_dir = Path(__file__).parent / 'screenshots'

    for lang in languages:
        lang_dir = base_dir / lang
        if not lang_dir.exists():
            print(f"⚠️  {lang} directory not found, skipping...")
            continue

        print(f"📝 Processing {lang}...")

        # 01-problem.html
        file_path = lang_dir / '01-problem.html'
        if file_path.exists():
            update_01_problem(file_path)
            print(f"  ✅ 01-problem.html")

        # 02-solution.html
        file_path = lang_dir / '02-solution.html'
        if file_path.exists():
            update_02_solution(file_path)
            print(f"  ✅ 02-solution.html")

        # 03-feature.html
        file_path = lang_dir / '03-feature.html'
        if file_path.exists():
            update_03_feature(file_path)
            print(f"  ✅ 03-feature.html")

        # 04-result.html
        file_path = lang_dir / '04-result.html'
        if file_path.exists():
            update_04_result(file_path)
            print(f"  ✅ 04-result.html")

    print("\n🎉 All screenshot styles updated successfully!")

if __name__ == '__main__':
    main()
