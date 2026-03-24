package com.example.demo.workflow.service;

import com.example.demo.workflow.dto.NodeTypeCreateRequest;
import com.example.demo.workflow.dto.NodeTypeResponse;
import com.example.demo.workflow.dto.NodeTypeUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 节点类型服务接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface NodeTypeService {

    /**
     * 创建节点类型
     *
     * @param request 创建请求
     * @return 节点类型响应
     */
    NodeTypeResponse createNodeType(NodeTypeCreateRequest request);

    /**
     * 根据ID获取节点类型
     *
     * @param id 节点类型ID
     * @return 节点类型响应
     */
    NodeTypeResponse getNodeTypeById(Long id);

    /**
     * 根据编码获取节点类型
     *
     * @param code 节点类型编码
     * @return 节点类型响应
     */
    NodeTypeResponse getNodeTypeByCode(String code);

    /**
     * 获取所有启用的节点类型
     *
     * @return 节点类型列表
     */
    List<NodeTypeResponse> getAllEnabledNodeTypes();

    /**
     * 根据分类获取节点类型
     *
     * @param category 分类
     * @return 节点类型列表
     */
    List<NodeTypeResponse> getNodeTypesByCategory(String category);

    /**
     * 分页获取节点类型
     *
     * @param pageable 分页参数
     * @return 节点类型分页列表
     */
    Page<NodeTypeResponse> getNodeTypeList(Pageable pageable);

    /**
     * 更新节点类型
     *
     * @param id      节点类型ID
     * @param request 更新请求
     * @return 节点类型响应
     */
    NodeTypeResponse updateNodeType(Long id, NodeTypeUpdateRequest request);

    /**
     * 删除节点类型
     *
     * @param id 节点类型ID
     */
    void deleteNodeType(Long id);

    /**
     * 启用/禁用节点类型
     *
     * @param id      节点类型ID
     * @param enabled 是否启用
     * @return 节点类型响应
     */
    NodeTypeResponse toggleNodeType(Long id, boolean enabled);
}
