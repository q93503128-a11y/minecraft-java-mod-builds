from pathlib import Path

path = Path('projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md')
text = path.read_text(encoding='utf-8')

housing_anchor = '- 왕도 세대교체 60년 비영구 투영에서 출생·승계·차세대 노동이 모두 발생하고 기존 77주택·156작업장·창립 명부를 훼손하지 않는지 확인하는 전용 fresh-world 회귀 검사\n'
housing_add = housing_anchor + (
    '- 왕도 차세대 주민의 결혼·사별·재혼과 배우자 가구 이동을 별도 혼인 장부로 유지하고, 작업장 배정은 보존한 채 임금이 이동한 가구 지갑을 따라가도록 연결한 혼인 생활주기\n'
    '- 28년 비영구 혼인 투영에서 신규 혼인 64쌍, 배우자 가구 이동 64건, 사별 후 재혼 1건을 확인하고 기존 77주택·156작업장을 유지하는 fresh-world 혼인 회귀 검사\n'
    '- 가문이 끊겨 비는 기존 77개 연립주택을 왕실 공실로 전환하고 차세대 부부의 독립 임차, 동적 임대료, 체납 완화, 장기 공실 이주민 입주, 미상속 재산 보존을 같은 77개 물리 지갑 슬롯에 연결하는 왕도 주거 시장\n'
    '- 72년 비영구 주거 투영에서 공실 6곳·점유 71곳, 77주택·77지갑 고정, 신규 합성 주택 0, 재입주 지갑 재사용과 이주 트랜잭션 롤백을 확인하는 fresh-world 주거 회귀 검사\n'
)
if '신규 혼인 64쌍' not in text:
    if housing_anchor not in text:
        raise SystemExit('capital lifecycle status anchor missing')
    text = text.replace(housing_anchor, housing_add, 1)

ecology_anchor = '- 외곽 가구에서 출생·성장·자연사·작업 승계와 결혼·재혼·배우자 가구 이동을 처리하고, 주택 소유·유지 적립금·공실·과밀 상태를 상속 뒤에도 보존하는 생활주기·혼인·부동산 오버레이\n'
ecology_add = ecology_anchor + (
    '- Living Kingdoms 자체 EntityType으로 등록된 은각사슴·재빛사냥개·강빛정령 3종과 북부 숲·서부 구릉·은빛강 지역 분리, 플레이어 주변 로드 청크 한정 스폰, 종별 로컬 개체수 상한을 갖춘 판타지 생태 1차 계층\n'
)
if '은각사슴·재빛사냥개·강빛정령' not in text:
    if ecology_anchor not in text:
        raise SystemExit('exterior lifecycle status anchor missing')
    text = text.replace(ecology_anchor, ecology_add, 1)

old_remaining = '- 왕도 차세대 주민의 결혼·재혼, 배우자 가구 이동, 독립 가구 형성·이주와 임대료·주택 공실 시장을 장기 세대교체에 연결하는 작업\n'
text = text.replace(old_remaining, '')

old_ecology = '- 전용 동물, 몬스터, 정령과 지역별 스폰 생태\n'
new_ecology = '- 1차 커스텀 종 3종을 전용 모델·텍스처·드롭·번식·군집 행동과 더 많은 지역종으로 확장하는 판타지 생태 완성 작업\n'
if old_ecology in text:
    text = text.replace(old_ecology, new_ecology, 1)

path.write_text(text, encoding='utf-8')
print('Refreshed Erden implementation status for capital housing and first custom fantasy species layer.')
