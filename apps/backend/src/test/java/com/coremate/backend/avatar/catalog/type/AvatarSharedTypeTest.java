package com.coremate.backend.avatar.catalog.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coremate.backend.global.exception.CoreMateException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("아바타 공유 타입 (2026-08-27 확정)")
class AvatarSharedTypeTest {

    @Nested
    @DisplayName("item_type 13종 / slot 12종")
    class TypeAndSlot {

        @Test
        @DisplayName("아이템 종류는 13종이다")
        void itemTypeHasThirteenValues() {
            assertThat(AvatarItemType.values()).hasSize(13);
        }

        @Test
        @DisplayName("슬롯은 12종이다 - FURNITURE 만 슬롯이 없다")
        void slotHasTwelveValues() {
            assertThat(AvatarSlot.values()).hasSize(12);
            assertThat(AvatarItemType.values()).hasSize(AvatarSlot.values().length + 1);
        }

        // Phase 1 에서 종류만 늘리고 슬롯을 안 늘리면 여기서 걸린다.
        @ParameterizedTest(name = "{0} 은 같은 이름의 슬롯을 갖는다")
        @EnumSource(value = AvatarItemType.class, names = "FURNITURE", mode = EnumSource.Mode.EXCLUDE)
        void everyEquippableTypeMapsToSlotOfSameName(AvatarItemType itemType) {
            assertThat(AvatarSlot.of(itemType)).isNotNull().hasToString(itemType.name());
        }

        @Test
        @DisplayName("FURNITURE 는 슬롯이 없다 - 좌표로 배치한다")
        void furnitureHasNoSlot() {
            assertThat(AvatarSlot.of(AvatarItemType.FURNITURE)).isNull();
            assertThat(AvatarItemType.FURNITURE.isFurniture()).isTrue();
        }

        @Test
        @DisplayName("폐기된 값은 더 이상 존재하지 않는다")
        void removedValuesAreGone() {
            assertThat(Arrays.stream(AvatarItemType.values()).map(Enum::name))
                    .doesNotContain("CLOTHES", "HEAD", "FACE", "BODY", "ACCESSORY",
                            "BACKGROUND", "ROOM_THEME", "ACTION_CARD", "OTHER");
        }
    }

    @Nested
    @DisplayName("필수 슬롯 6종은 해제할 수 없다")
    class RequiredSlot {

        // 2026-08-30 개정. 상의·하의가 비면 옷을 안 입은 아바타가 되고, 방 테마가 비면 방이 안 그려진다.
        // FLOOR 는 WALLPAPER 테마에 흡수돼 더 이상 쓰지 않으므로 필수가 아니다.
        @Test
        @DisplayName("필수 슬롯은 HAIR, EYES, MOUTH, TOP, BOTTOM, WALLPAPER 뿐이다")
        void requiredSlotsAreExactlySix() {
            assertThat(Arrays.stream(AvatarSlot.values()).filter(AvatarSlot::isRequired))
                    .containsExactlyInAnyOrder(AvatarSlot.HAIR, AvatarSlot.EYES, AvatarSlot.MOUTH,
                            AvatarSlot.TOP, AvatarSlot.BOTTOM, AvatarSlot.WALLPAPER);
        }
    }

    @Nested
    @DisplayName("렌더 순서는 서버가 갖는다")
    class RenderOrder {

        @Test
        @DisplayName("착용 9부위는 확정된 정면 겹침 순서를 갖는다")
        void avatarStackIsInSpecOrder() {
            assertThat(Arrays.stream(AvatarSlot.values())
                    .filter(s -> s.getRenderOrder() != null)
                    .sorted(Comparator.comparing(AvatarSlot::getRenderOrder))
                    .map(Enum::name))
                    .containsExactly("SHOES", "BOTTOM", "TOP", "BAG",
                            "HAIR", "EYES", "MOUTH", "EYEWEAR", "HAT");
        }

        @Test
        @DisplayName("아바타 스택이 아닌 3종은 순서가 없다")
        void nonAvatarSlotsHaveNoRenderOrder() {
            assertThat(Arrays.stream(AvatarSlot.values())
                    .filter(s -> s.getRenderOrder() == null)
                    .map(Enum::name))
                    .containsExactlyInAnyOrder("PET", "WALLPAPER", "FLOOR");
        }

        @Test
        @DisplayName("순서는 1부터 9까지 중복 없이 이어진다")
        void renderOrderIsDenseAndUnique() {
            assertThat(Arrays.stream(AvatarSlot.values())
                    .map(AvatarSlot::getRenderOrder)
                    .filter(Objects::nonNull))
                    .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9);
        }
    }

    @Nested
    @DisplayName("등급별 중복 환급 코인")
    class Rarity {

        @ParameterizedTest(name = "{0} 중복 시 {1} 코인")
        @CsvSource({"COMMON, 20", "RARE, 50", "EPIC, 100", "LEGENDARY, 200"})
        void refundCoinPerRarity(ItemRarity rarity, int expected) {
            assertThat(rarity.getRefundCoin()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("from() 은 모르는 값을 삼키지 않는다")
    class Parsing {

        @ParameterizedTest
        @ValueSource(strings = {" hair ", "hair", "HAIR"})
        @DisplayName("공백과 대소문자는 정규화한다")
        void normalizesInput(String raw) {
            assertThat(AvatarItemType.from(raw)).isEqualTo(AvatarItemType.HAIR);
            assertThat(AvatarSlot.from(raw)).isEqualTo(AvatarSlot.HAIR);
        }

        @ParameterizedTest
        @ValueSource(strings = {"HEAD", "CLOTHES", "OTHER", "", "   "})
        @DisplayName("폐기된 값과 빈 값은 예외 - 예전엔 OTHER 로 삼켰다")
        void throwsOnUnknown(String raw) {
            assertThatThrownBy(() -> AvatarItemType.from(raw)).isInstanceOf(CoreMateException.class);
            assertThatThrownBy(() -> AvatarSlot.from(raw)).isInstanceOf(CoreMateException.class);
            assertThatThrownBy(() -> ItemRarity.from(raw)).isInstanceOf(CoreMateException.class);
        }

        @Test
        @DisplayName("null 도 예외")
        void throwsOnNull() {
            assertThatThrownBy(() -> AvatarItemType.from(null)).isInstanceOf(CoreMateException.class);
            assertThatThrownBy(() -> AvatarSlot.from(null)).isInstanceOf(CoreMateException.class);
            assertThatThrownBy(() -> ItemRarity.from(null)).isInstanceOf(CoreMateException.class);
        }
    }

    @Nested
    @DisplayName("획득 경로")
    class Source {

        @Test
        @DisplayName("현재 쓰이는 경로는 3종 - 이름은 user_items.acquired_source 에 그대로 저장된다")
        void acquiredSourceValues() {
            assertThat(Arrays.stream(AcquiredSource.values()).map(Enum::name))
                    .containsExactlyInAnyOrder("RANDOM_BOX", "SHOP_PURCHASE", "DEFAULT_GRANT");
        }
    }
}
