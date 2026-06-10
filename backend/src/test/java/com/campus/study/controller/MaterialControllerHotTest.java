package com.campus.study.controller;

import com.campus.study.entity.Material;
import com.campus.study.service.FavoriteService;
import com.campus.study.service.MaterialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaterialController.class)
class MaterialControllerHotTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaterialService materialService;

    @MockBean
    private FavoriteService favoriteService;

    private Material buildMaterial(Long id, String title, int viewCount) {
        Material material = new Material();
        material.setId(id);
        material.setTitle(title);
        material.setDescription("test description");
        material.setViewCount(viewCount);
        material.setDownloadCount(0);
        material.setStatus(1);
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        return material;
    }

    @Nested
    @DisplayName("窗口无热度数据场景")
    class EmptyHotListTests {

        @BeforeEach
        void setUp() {
            when(materialService.getHotMaterials7Days()).thenReturn(new ArrayList<>());
            when(materialService.getHotMaterials30Days()).thenReturn(new ArrayList<>());
            when(materialService.getHotMaterialsSemester()).thenReturn(new ArrayList<>());

            Map<String, Object> all = new HashMap<>();
            all.put("hot7d", new ArrayList<>());
            all.put("hot30d", new ArrayList<>());
            all.put("hotSemester", new ArrayList<>());
            when(materialService.getAllHotMaterials()).thenReturn(all);
        }

        @Test
        @DisplayName("[首页三Tab联动] 无range参数时返回三种榜单，全部为空列表，首页展示'暂无数据'")
        void testGetAllHotRangesEmpty() throws Exception {
            mockMvc.perform(get("/materials/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.hot7d").isArray())
                    .andExpect(jsonPath("$.data.hot7d", hasSize(0)))
                    .andExpect(jsonPath("$.data.hot30d").isArray())
                    .andExpect(jsonPath("$.data.hot30d", hasSize(0)))
                    .andExpect(jsonPath("$.data.hotSemester").isArray())
                    .andExpect(jsonPath("$.data.hotSemester", hasSize(0)));

            verify(materialService).getAllHotMaterials();
        }

        @Test
        @DisplayName("[近7天Tab] range=7d 返回空列表，首页近7天tab展示'暂无数据'")
        void testGetHot7dEmpty() throws Exception {
            mockMvc.perform(get("/materials/hot").param("range", "7d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(materialService).getHotMaterials7Days();
        }

        @Test
        @DisplayName("[近30天Tab] range=30d 返回空列表，首页近30天tab展示'暂无数据'")
        void testGetHot30dEmpty() throws Exception {
            mockMvc.perform(get("/materials/hot").param("range", "30d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(materialService).getHotMaterials30Days();
        }

        @Test
        @DisplayName("[本学期Tab] range=semester 返回空列表，首页本学期tab展示'暂无数据'")
        void testGetHotSemesterEmpty() throws Exception {
            mockMvc.perform(get("/materials/hot").param("range", "semester"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(materialService).getHotMaterialsSemester();
        }
    }

    @Nested
    @DisplayName("窗口仅1条热度数据场景")
    class SingleHotItemTests {

        private Material hotMaterial;

        @BeforeEach
        void setUp() {
            hotMaterial = buildMaterial(1L, "高一数学必修一第一章知识点总结", 156);

            when(materialService.getHotMaterials7Days()).thenReturn(List.of(hotMaterial));
            when(materialService.getHotMaterials30Days()).thenReturn(List.of(hotMaterial));
            when(materialService.getHotMaterialsSemester()).thenReturn(List.of(hotMaterial));

            Map<String, Object> all = new HashMap<>();
            all.put("hot7d", List.of(hotMaterial));
            all.put("hot30d", List.of(hotMaterial));
            all.put("hotSemester", List.of(hotMaterial));
            when(materialService.getAllHotMaterials()).thenReturn(all);
        }

        @Test
        @DisplayName("[首页三Tab联动] 无range参数时返回三种榜单，各含1条热度数据")
        void testGetAllHotRangesSingleItem() throws Exception {
            mockMvc.perform(get("/materials/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.hot7d", hasSize(1)))
                    .andExpect(jsonPath("$.data.hot7d[0].id").value(1))
                    .andExpect(jsonPath("$.data.hot7d[0].title").value("高一数学必修一第一章知识点总结"))
                    .andExpect(jsonPath("$.data.hot7d[0].viewCount").value(156))
                    .andExpect(jsonPath("$.data.hot30d", hasSize(1)))
                    .andExpect(jsonPath("$.data.hot30d[0].id").value(1))
                    .andExpect(jsonPath("$.data.hotSemester", hasSize(1)))
                    .andExpect(jsonPath("$.data.hotSemester[0].id").value(1));

            verify(materialService).getAllHotMaterials();
        }

        @Test
        @DisplayName("[近7天Tab] range=7d 仅1条数据，viewCount>0，正常展示")
        void testGetHot7dSingleItem() throws Exception {
            mockMvc.perform(get("/materials/hot").param("range", "7d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("高一数学必修一第一章知识点总结"))
                    .andExpect(jsonPath("$.data[0].viewCount").value(greaterThan(0)));

            verify(materialService).getHotMaterials7Days();
        }

        @Test
        @DisplayName("[近30天Tab] range=30d 仅1条数据，viewCount>0，正常展示")
        void testGetHot30dSingleItem() throws Exception {
            mockMvc.perform(get("/materials/hot").param("range", "30d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].viewCount").value(greaterThan(0)));

            verify(materialService).getHotMaterials30Days();
        }

        @Test
        @DisplayName("[本学期Tab] range=semester 仅1条数据，viewCount>0，正常展示")
        void testGetHotSemesterSingleItem() throws Exception {
            mockMvc.perform(get("/materials/hot").param("range", "semester"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].viewCount").value(greaterThan(0)));

            verify(materialService).getHotMaterialsSemester();
        }
    }

    @Nested
    @DisplayName("viewCount=0 过滤校验场景")
    class ZeroViewCountFilterTests {

        @Test
        @DisplayName("[近7天Tab] viewCount=0 的资料不会进入榜单，列表为空")
        void testViewCountZeroFilteredOut7d() throws Exception {
            Material zeroViewMaterial = buildMaterial(99L, "冷门资料", 0);
            when(materialService.getHotMaterials7Days()).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/materials/hot").param("range", "7d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(materialService).getHotMaterials7Days();
        }

        @Test
        @DisplayName("[近30天Tab] viewCount=0 的资料不会进入榜单，列表为空")
        void testViewCountZeroFilteredOut30d() throws Exception {
            when(materialService.getHotMaterials30Days()).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/materials/hot").param("range", "30d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(materialService).getHotMaterials30Days();
        }

        @Test
        @DisplayName("[本学期Tab] viewCount=0 的资料不会进入榜单，列表为空")
        void testViewCountZeroFilteredOutSemester() throws Exception {
            when(materialService.getHotMaterialsSemester()).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/materials/hot").param("range", "semester"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(materialService).getHotMaterialsSemester();
        }
    }

    @Nested
    @DisplayName("多个热度数据排序场景")
    class MultipleHotItemsSortTests {

        @Test
        @DisplayName("[近7天Tab] 多条热度数据按viewCount降序排列")
        void testHot7dSortedByViewCountDesc() throws Exception {
            Material m1 = buildMaterial(1L, "资料A", 300);
            Material m2 = buildMaterial(2L, "资料B", 200);
            Material m3 = buildMaterial(3L, "资料C", 100);
            when(materialService.getHotMaterials7Days()).thenReturn(List.of(m1, m2, m3));

            mockMvc.perform(get("/materials/hot").param("range", "7d"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].viewCount").value(300))
                    .andExpect(jsonPath("$.data[1].viewCount").value(200))
                    .andExpect(jsonPath("$.data[2].viewCount").value(100));

            verify(materialService).getHotMaterials7Days();
        }
    }
}
