#!/usr/bin/env python3

import hmac
import hashlib
import random
import string
from datetime import datetime

# PromoCodeValidator와 동일한 비밀키
SECRET_KEY = "unshort_lifetime_premium_secret_2024"

def generate_signature(id_str):
    """ID에 대한 HMAC-SHA256 서명 생성"""
    mac = hmac.new(
        SECRET_KEY.encode('utf-8'),
        id_str.encode('utf-8'),
        hashlib.sha256
    )
    hash_bytes = mac.digest()
    # 앞 4바이트를 8자리 HEX로 변환
    return ''.join(f'{b:02X}' for b in hash_bytes[:4])

def generate_promo_code(id_str):
    """프로모션 코드 생성"""
    upper_id = id_str.upper()
    signature = generate_signature(upper_id)
    return f"UNSHORT-{upper_id}-{signature}"

def generate_random_id(prefix, index):
    """랜덤 ID 생성"""
    random_part = ''.join(random.choices(string.ascii_uppercase + string.digits, k=4))
    return f"{prefix}{index:03d}{random_part}"

def main():
    count = 10
    prefix = "FRIEND"

    print("=" * 60)
    print("Lifetime Premium 프로모션 코드 생성")
    print("=" * 60)
    print(f"개수: {count}")
    print(f"접두사: {prefix}")
    print("=" * 60)
    print()

    codes = []

    for i in range(1, count + 1):
        id_str = generate_random_id(prefix, i)
        code = generate_promo_code(id_str)
        codes.append((i, id_str, code))
        print(f"[{i}/{count}] {code}")

    print()
    print("=" * 60)

    # CSV 파일로 저장
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"/Users/muuu/Development/shortblock/scripts/promo_codes_{timestamp}.csv"

    with open(filename, 'w', encoding='utf-8') as f:
        f.write("번호,ID,프로모션 코드,생성일시\n")
        for index, id_str, code in codes:
            f.write(f"{index},{id_str},{code},{timestamp}\n")

    print(f"✅ {count}개의 프로모션 코드가 생성되었습니다!")
    print(f"📁 파일 저장됨: {filename}")
    print("=" * 60)

if __name__ == "__main__":
    main()
