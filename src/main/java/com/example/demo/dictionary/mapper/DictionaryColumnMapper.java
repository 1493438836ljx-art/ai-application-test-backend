package com.example.demo.dictionary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dictionary.entity.DictionaryColumn;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字段定义Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface DictionaryColumnMapper extends BaseMapper<DictionaryColumn> {

    /**
     * 根据数据字典ID查询字段列表
     *
     * @param dictionaryId 数据字典ID
     * @return 字段列表
     */
    @Select("SELECT * FROM dictionary_column WHERE dictionary_id = #{dictionaryId} ORDER BY sort_order ASC")
    List<DictionaryColumn> selectByDictionaryId(@Param("dictionaryId") Long dictionaryId);

    /**
     * 根据数据字典ID删除字段
     *
     * @param dictionaryId 数据字典ID
     * @return 删除数量
     */
    @Delete("DELETE FROM dictionary_column WHERE dictionary_id = #{dictionaryId}")
    int deleteByDictionaryId(@Param("dictionaryId") Long dictionaryId);

    /**
     * 统计数据字典的字段数量
     *
     * @param dictionaryId 数据字典ID
     * @return 字段数量
     */
    @Select("SELECT COUNT(*) FROM dictionary_column WHERE dictionary_id = #{dictionaryId}")
    int countByDictionaryId(@Param("dictionaryId") Long dictionaryId);
}
