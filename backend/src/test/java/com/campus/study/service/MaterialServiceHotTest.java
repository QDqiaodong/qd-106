package com.campus.study.service;

import com.campus.study.entity.Material;
import com.campus.study.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialServiceHotTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MaterialService materialService;

    private Material buildMaterial(Long id, String title, int dbViewCount) {
        Material material = new Material();
        material.setId(id);
        material.setTitle(title);
        material.setDescription("test");
        material.setViewCount(dbViewCount);
        material.setDownloadCount(0);
        material.setStatus(1);
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        return material;
    }

    private List<Material> buildAllMaterials() {
        List<Material> list = new ArrayList<>();
        list.add(buildMaterial(1L, "资料A", 100));
        list.add(buildMaterial(2L, "资料B", 200));
        list.add(buildMaterial(3L, "资料C", 50));
        return list;
    }

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Nested
    @DisplayName("窗口无热度数据场景")
    class EmptyRangeViewCountsTests {

        @Test
        @DisplayName("[近7天榜] 时间窗口内无任何浏览量数据时，返回空列表")
        void testGetHot7dReturnsEmptyWhenNoRangeData() {
            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new HashMap<>());

            List<Material> result = materialService.getHotMaterials7Days();

            assertNotNull(result);
            assertTrue(result.isEmpty(), "窗口无热度数据时应返回空列表");
            verify(viewCountService).getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("[近30天榜] 时间窗口内无任何浏览量数据时，返回空列表")
        void testGetHot30dReturnsEmptyWhenNoRangeData() {
            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new HashMap<>());

            List<Material> result = materialService.getHotMaterials30Days();

            assertNotNull(result);
            assertTrue(result.isEmpty(), "窗口无热度数据时应返回空列表");
        }

        @Test
        @DisplayName("[本学期榜] 时间窗口内无任何浏览量数据时，返回空列表")
        void testGetHotSemesterReturnsEmptyWhenNoRangeData() {
            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new HashMap<>());

            List<Material> result = materialService.getHotMaterialsSemester();

            assertNotNull(result);
            assertTrue(result.isEmpty(), "窗口无热度数据时应返回空列表");
        }

        @Test
        @DisplayName("[首页三Tab联动] getAllHotMaterials 返回的三个榜单均为空")
        void testGetAllHotMaterialsAllEmpty() {
            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new HashMap<>());

            Map<String, Object> result = materialService.getAllHotMaterials();

            assertNotNull(result);
            assertTrue(((List<?>) result.get("hot7d")).isEmpty(), "近7天榜应为空");
            assertTrue(((List<?>) result.get("hot30d")).isEmpty(), "近30天榜应为空");
            assertTrue(((List<?>) result.get("hotSemester")).isEmpty(), "本学期榜应为空");
        }
    }

    @Nested
    @DisplayName("窗口仅1条热度数据场景")
    class SingleRangeViewCountTests {

        @Test
        @DisplayName("[近7天榜] 窗口内仅有1条浏览量>0的资料，返回单元素列表")
        void testGetHot7dSingleItem() {
            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(1L, 50);

            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterials7Days();

            assertNotNull(result);
            assertEquals(1, result.size(), "应仅返回1条有热度的资料");
            assertEquals(1L, result.get(0).getId());
            assertEquals(50, result.get(0).getViewCount(), "viewCount 应为窗口内热度值");
        }

        @Test
        @DisplayName("[近30天榜] 窗口内仅有1条浏览量>0的资料，返回单元素列表")
        void testGetHot30dSingleItem() {
            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(2L, 120);

            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterials30Days();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals(120, result.get(0).getViewCount());
        }

        @Test
        @DisplayName("[本学期榜] 窗口内仅有1条浏览量>0的资料，返回单元素列表")
        void testGetHotSemesterSingleItem() {
            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(3L, 80);

            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterialsSemester();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(3L, result.get(0).getId());
            assertEquals(80, result.get(0).getViewCount());
        }
    }

    @Nested
    @DisplayName("viewCount=0 过滤场景")
    class ZeroViewCountFilterTests {

        @Test
        @DisplayName("[近7天榜] viewCount=0 的资料被过滤掉，不进入榜单")
        void testViewCountZeroFilteredOut7d() {
            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(1L, 0);
            rangeCounts.put(2L, 0);

            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterials7Days();

            assertNotNull(result);
            assertTrue(result.isEmpty(), "viewCount=0 的资料不应进入榜单");
        }

        @Test
        @DisplayName("[近30天榜] viewCount>0 和 viewCount=0 混合时，仅保留>0的")
        void testMixedViewCountFiltered30d() {
            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(1L, 0);
            rangeCounts.put(2L, 150);
            rangeCounts.put(3L, 0);

            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterials30Days();

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals(150, result.get(0).getViewCount());
        }

        @Test
        @DisplayName("[本学期榜] 已下架资料即使有热度也不进入榜单")
        void testOfflineMaterialFilteredOutSemester() {
            Material offlineMaterial = buildMaterial(99L, "已下架资料", 999);
            offlineMaterial.setStatus(0);

            List<Material> allMaterials = new ArrayList<>();
            allMaterials.add(buildMaterial(1L, "正常资料A", 100));
            allMaterials.add(offlineMaterial);

            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(1L, 50);
            rangeCounts.put(99L, 999);

            when(materialRepository.findByStatus(1)).thenReturn(List.of(buildMaterial(1L, "正常资料A", 100)));
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterialsSemester();

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }
    }

    @Nested
    @DisplayName("多数据排序场景")
    class MultipleItemsSortTests {

        @Test
        @DisplayName("[近7天榜] 多条数据按viewCount降序排列")
        void testHot7dSortedByViewCountDesc() {
            Map<Long, Integer> rangeCounts = new HashMap<>();
            rangeCounts.put(1L, 100);
            rangeCounts.put(2L, 300);
            rangeCounts.put(3L, 200);

            when(materialRepository.findByStatus(1)).thenReturn(buildAllMaterials());
            when(viewCountService.getViewCountsInDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(rangeCounts);

            List<Material> result = materialService.getHotMaterials7Days();

            assertEquals(3, result.size());
            assertEquals(300, result.get(0).getViewCount());
            assertEquals(200, result.get(1).getViewCount());
            assertEquals(100, result.get(2).getViewCount());
            assertEquals(2L, result.get(0).getId());
            assertEquals(3L, result.get(1).getId());
            assertEquals(1L, result.get(2).getId());
        }
    }
}
