package com.helpmanual.service;

import com.helpmanual.dto.request.ReorderRequest;
import com.helpmanual.dto.response.CategoryTreeNode;
import com.helpmanual.entity.Category;
import com.helpmanual.exception.BadRequestException;
import com.helpmanual.exception.ResourceNotFoundException;
import com.helpmanual.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public Category create(String name, String slug, Long parentId, String icon, Integer sortOrder) {
        if (!StringUtils.hasText(slug)) {
            slug = generateSlug(name);
        }
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category slug already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setParentId(parentId);
        category.setIcon(icon);
        category.setSortOrder(sortOrder != null ? sortOrder : 0);

        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public Category update(Long id, String name, String slug, Long parentId, String icon, Integer sortOrder) {
        Category category = findById(id);

        if (StringUtils.hasText(name)) {
            category.setName(name);
        }
        if (StringUtils.hasText(slug) && !slug.equals(category.getSlug())) {
            if (categoryRepository.existsBySlug(slug)) {
                throw new BadRequestException("Category slug already exists");
            }
            category.setSlug(slug);
        }
        category.setParentId(parentId);
        if (icon != null) {
            category.setIcon(icon);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public void delete(Long id) {
        Category category = findById(id);
        List<Category> children = categoryRepository.findByParentIdOrderBySortOrder(id);
        if (!children.isEmpty()) {
            throw new BadRequestException("Cannot delete category with subcategories");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public void reorder(List<ReorderRequest.ReorderItem> items) {
        for (ReorderRequest.ReorderItem item : items) {
            Category category = findById(item.id());
            category.setSortOrder(item.sortOrder());
            categoryRepository.save(category);
        }
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public Category toggleVisibility(Long id, boolean visible) {
        Category category = findById(id);
        category.setVisible(visible);
        return categoryRepository.save(category);
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Cacheable("categoryTree")
    public List<CategoryTreeNode> buildTree(boolean visibleOnly) {
        List<Category> categories;
        if (visibleOnly) {
            categories = categoryRepository.findByVisibleTrueOrderBySortOrder();
        } else {
            categories = categoryRepository.findAll();
            categories.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        Map<Long, List<Category>> byParent = categories.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getParentId() != null ? c.getParentId() : 0L
                ));

        return buildChildren(byParent, 0L);
    }

    private List<CategoryTreeNode> buildChildren(Map<Long, List<Category>> byParent, Long parentId) {
        List<Category> children = byParent.getOrDefault(parentId, List.of());
        List<CategoryTreeNode> nodes = new ArrayList<>();

        for (Category c : children) {
            CategoryTreeNode node = new CategoryTreeNode(
                    c.getId(), c.getParentId(), c.getName(), c.getSlug(),
                    c.getIcon(), c.getSortOrder(), c.isVisible()
            );
            node.setChildren(buildChildren(byParent, c.getId()));
            nodes.add(node);
        }

        return nodes;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "-")
                .replaceAll("^-|-$", "");
    }
}
