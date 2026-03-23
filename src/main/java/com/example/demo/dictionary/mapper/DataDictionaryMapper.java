package com.example.demo.dictionary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dictionary.entity.DataDictionary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据字典Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface DataDictionaryMapper extends BaseMapper<DataDictionary> {

    /**
     * 根据关键字搜索（匹配名称、字段key、字段label）
     *
     * @param page    分页参数
     * @param keyword 关键字
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT DISTINCT d.* FROM data_dictionary d " +
            "LEFT JOIN dictionary_column c ON d.id = c.dictionary_id " +
            "WHERE d.is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (d.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR c.column_key LIKE CONCAT('%', #{keyword}, '%') " +
            "OR c.column_label LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY d.created_at DESC" +
            "</script>")
    IPage<DataDictionary> searchByKeyword(Page<DataDictionary> page, @Param("keyword") String keyword);

    /**
     * 获取所有未删除的数据字典（用于下拉列表）
     *
     * @return 数据字典列表
     */
    @Select("SELECT * FROM data_dictionary WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<DataDictionary> selectAllForDropdown();
}
