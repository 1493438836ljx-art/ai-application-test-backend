package com.example.demo.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.workflow.dto.NodeTypeCreateRequest;
import com.example.demo.workflow.dto.NodeTypeResponse;
import com.example.demo.workflow.dto.NodeTypeUpdateRequest;
import com.example.demo.workflow.entity.WorkflowNodeTypeEntity;
import com.example.demo.workflow.mapper.WorkflowNodeTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点类型服务实现类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeTypeServiceImpl implements NodeTypeService {

    private final WorkflowNodeTypeMapper nodeTypeMapper;

    @Override
    @Transactional
    public NodeTypeResponse createNodeType(NodeTypeCreateRequest request) {
        log.info("创建节点类型: {}", request.getCode());

        // 检查编码是否已存在
        if (nodeTypeMapper.existsByCode(request.getCode())) {
            throw BusinessException.conflict("节点类型编码已存在: " + request.getCode());
        }

        WorkflowNodeTypeEntity entity = new WorkflowNodeTypeEntity();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setCategory(request.getCategory());
        entity.setDescription(request.getDescription());
        entity.setIcon(request.getIcon());
        entity.setDefaultConfig(request.getDefaultConfig());
        entity.setInputPorts(request.getInputPorts());
        entity.setOutputPorts(request.getOutputPorts());
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        nodeTypeMapper.insert(entity);
        return NodeTypeResponse.fromEntity(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public NodeTypeResponse getNodeTypeById(Long id) {
        WorkflowNodeTypeEntity entity = nodeTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("节点类型", id);
        }
        return NodeTypeResponse.fromEntity(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public NodeTypeResponse getNodeTypeByCode(String code) {
        return nodeTypeMapper.selectByCode(code)
                .map(NodeTypeResponse::fromEntity)
                .orElseThrow(() -> BusinessException.notFound("节点类型", code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NodeTypeResponse> getAllEnabledNodeTypes() {
        return nodeTypeMapper.selectEnabled().stream()
                .map(NodeTypeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NodeTypeResponse> getNodeTypesByCategory(String category) {
        return nodeTypeMapper.selectByCategory(category).stream()
                .map(NodeTypeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<NodeTypeResponse> getNodeTypeList(org.springframework.data.domain.Pageable pageable) {
        Page<WorkflowNodeTypeEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        IPage<WorkflowNodeTypeEntity> result = nodeTypeMapper.selectPage(page,
                new LambdaQueryWrapper<WorkflowNodeTypeEntity>()
                        .orderByAsc(WorkflowNodeTypeEntity::getSortOrder)
                        .orderByDesc(WorkflowNodeTypeEntity::getCreatedAt));

        List<NodeTypeResponse> content = result.getRecords().stream()
                .map(NodeTypeResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, result.getTotal());
    }

    @Override
    @Transactional
    public NodeTypeResponse updateNodeType(Long id, NodeTypeUpdateRequest request) {
        log.info("更新节点类型: {}", id);

        WorkflowNodeTypeEntity entity = nodeTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("节点类型", id);
        }

        // 更新字段
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }
        if (request.getDefaultConfig() != null) {
            entity.setDefaultConfig(request.getDefaultConfig());
        }
        if (request.getInputPorts() != null) {
            entity.setInputPorts(request.getInputPorts());
        }
        if (request.getOutputPorts() != null) {
            entity.setOutputPorts(request.getOutputPorts());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        nodeTypeMapper.updateById(entity);
        return NodeTypeResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public void deleteNodeType(Long id) {
        log.info("删除节点类型: {}", id);

        WorkflowNodeTypeEntity entity = nodeTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("节点类型", id);
        }

        nodeTypeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public NodeTypeResponse toggleNodeType(Long id, boolean enabled) {
        log.info("{}节点类型: {}", enabled ? "启用" : "禁用", id);

        WorkflowNodeTypeEntity entity = nodeTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("节点类型", id);
        }

        entity.setEnabled(enabled);
        nodeTypeMapper.updateById(entity);
        return NodeTypeResponse.fromEntity(entity);
    }
}
