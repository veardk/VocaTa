package com.vocata.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.user.dto.response.FavoriteResponse;
import com.vocata.user.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户收藏Mapper接口
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 分页查询用户收藏的角色列表
     * @param page 分页对象
     * @param userId 用户ID
     * @return 收藏基础信息列表
     */
    @Select("""
        SELECT
            uf.id,
            uf.user_id,
            uf.character_id,
            uf.created_at
        FROM vocata_user_favorite uf
        LEFT JOIN vocata_character c ON uf.character_id = c.id
        WHERE uf.user_id = #{userId}
          AND c.is_delete = 0
          AND c.status = 1
        ORDER BY uf.created_at DESC
        """)
    Page<UserFavorite> getFavoritesByUserId(Page<UserFavorite> page, @Param("userId") Long userId);

    /**
     * 获取用户收藏数量
     * @param userId 用户ID
     * @return 收藏数量
     */
    @Select("""
        SELECT COUNT(1)
        FROM vocata_user_favorite uf
        LEFT JOIN vocata_character c ON uf.character_id = c.id
        WHERE uf.user_id = #{userId}
          AND c.is_delete = 0
          AND c.status = 1
        """)
    Integer getFavoriteCountByUserId(@Param("userId") Long userId);

    /**
     * 批量检查收藏状态
     * @param userId 用户ID
     * @param characterIds 角色ID列表
     * @return 收藏状态Map，key为characterId，value为是否收藏
     */
    @Select("""
        <script>
        SELECT character_id, 1 as is_favorite
        FROM vocata_user_favorite
        WHERE user_id = #{userId}
          AND character_id IN
          <foreach collection="characterIds" item="characterId" open="(" separator="," close=")">
            #{characterId}
          </foreach>
        </script>
        """)
    List<Map<String, Object>> batchCheckFavoriteStatus(@Param("userId") Long userId, @Param("characterIds") List<Long> characterIds);

    /**
     * 获取角色收藏数排行榜
     * @param limit 限制数量
     * @return 角色收藏数排行，包含角色ID和收藏数
     */
    @Select("""
        SELECT
            c.id as character_id,
            c.character_code,
            c.name,
            c.avatar_url,
            COUNT(uf.id) as favorite_count
        FROM vocata_character c
        LEFT JOIN vocata_user_favorite uf ON c.id = uf.character_id
        WHERE c.is_delete = 0
          AND c.status = 1
          AND c.is_private = false
        GROUP BY c.id, c.character_code, c.name, c.avatar_url
        ORDER BY favorite_count DESC, c.created_at DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> getFavoriteRanking(@Param("limit") Integer limit);

    /**
     * 检查用户是否已收藏指定角色
     * @param userId 用户ID
     * @param characterId 角色ID
     * @return 收藏记录ID，不存在返回null
     */
    @Select("""
        SELECT id
        FROM vocata_user_favorite
        WHERE user_id = #{userId}
          AND character_id = #{characterId}
        """)
    Long checkUserFavorite(@Param("userId") Long userId, @Param("characterId") Long characterId);
}