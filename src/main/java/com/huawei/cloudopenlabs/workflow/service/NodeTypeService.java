/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.service;

import com.huawei.cloudopenlabs.workflow.dto.NodeTypeCreateRequest;
import com.huawei.cloudopenlabs.workflow.dto.NodeTypeResponse;
import com.huawei.cloudopenlabs.workflow.dto.NodeTypeUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 节点类型服务接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
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
    NodeTypeResponse getNodeTypeById(String id);

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
    NodeTypeResponse updateNodeType(String id, NodeTypeUpdateRequest request);

    /**
     * 删除节点类型
     *
     * @param id 节点类型ID
     */
    void deleteNodeType(String id);

    /**
     * 启用/禁用节点类型
     *
     * @param id      节点类型ID
     * @param enabled 是否启用
     * @return 节点类型响应
     */
    NodeTypeResponse toggleNodeType(String id, boolean enabled);
}
