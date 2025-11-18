#!/usr/bin/env python3
"""
Feature Graphic HTML 파일들의 스타일을 미니멀 화이트 베이스로 일괄 업데이트
"""
import os
import re
from pathlib import Path

# 언어 디렉토리 목록
languages = [
    'en', 'ja', 'zh-cn', 'zh-tw', 'vi', 'th', 'in', 'hi',
    'es', 'fr', 'de', 'pt', 'ru', 'it', 'ar', 'tr'
]

def update_feature_graphic(file_path):
    """feature-graphic.html 스타일 업데이트"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # body background 변경
    content = re.sub(
        r'background:\s*#f5f5f5;',
        'background: #F5F5F5;',
        content
    )

    # feature-graphic background 변경 (그라데이션 제거, 그림자 제거)
    content = re.sub(
        r'\.feature-graphic\s*\{[^}]*\}',
        '''.feature-graphic {
            width: 1024px;
            height: 500px;
            background: #FFFFFF;
            position: relative;
            overflow: hidden;
        }''',
        content,
        flags=re.DOTALL
    )

    # bg-pattern 제거
    content = re.sub(
        r'/\*\s*Background Pattern\s*\*/\s*\.bg-pattern\s*\{[^}]*\}',
        '',
        content,
        flags=re.DOTALL
    )

    # HTML에서 bg-pattern div 제거
    content = re.sub(
        r'<!--\s*Background Pattern\s*-->\s*<div class="bg-pattern"></div>\s*',
        '',
        content
    )

    # logo 색상 변경
    content = re.sub(
        r'(\.logo\s*\{[^}]*color:\s*)#FFFFFF',
        r'\g<1>#1A1A1A',
        content
    )

    # main-message 색상 변경
    content = re.sub(
        r'(\.main-message\s*\{[^}]*color:\s*)#FFFFFF',
        r'\g<1>#1A1A1A',
        content
    )

    # sub-message 업데이트
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*font-size:\s*)20px',
        r'\g<1>18px',
        content
    )
    content = re.sub(
        r'(\.sub-message\s*\{[^}]*color:\s*)#D1D5DB',
        r'\g<1>#6B7280',
        content
    )

    # feature-icon 배경 변경
    content = re.sub(
        r'(\.feature-icon\s*\{[^}]*background:\s*)rgba\(139,\s*92,\s*246,\s*0\.2\)',
        r'\g<1>#F3F4F6',
        content
    )

    # feature-text 색상 변경
    content = re.sub(
        r'(\.feature-text\s*\{[^}]*color:\s*)#F3F4F6',
        r'\g<1>#1A1A1A',
        content
    )

    # timer-circle 업데이트
    content = re.sub(
        r'\.timer-circle\s*\{[^}]*\}',
        '''.timer-circle {
            width: 200px;
            height: 200px;
            border-radius: 50%;
            background: #F9FAFB;
            border: 3px solid #8B5CF6;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
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

    # timer-label 업데이트
    content = re.sub(
        r'(\.timer-label\s*\{[^}]*font-weight:\s*)600',
        r'\g<1>500',
        content
    )
    content = re.sub(
        r'(\.timer-label\s*\{[^}]*color:\s*)#D1D5DB',
        r'\g<1>#6B7280',
        content
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    base_dir = Path(__file__).parent / 'feature-graphics'

    for lang in languages:
        lang_dir = base_dir / lang
        file_path = lang_dir / 'feature-graphic.html'

        if not file_path.exists():
            print(f"⚠️  {lang}/feature-graphic.html not found, skipping...")
            continue

        print(f"📝 Processing {lang}...")
        update_feature_graphic(file_path)
        print(f"  ✅ feature-graphic.html")

    print("\n🎉 All feature graphic styles updated successfully!")

if __name__ == '__main__':
    main()
