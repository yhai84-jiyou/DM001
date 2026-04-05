package com.helpmanual.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CategoryTreeNode {
    private Long id;
    private Long parentId;
    private String name;
    private String slug;
    private String icon;
    private int sortOrder;
    private boolean visible;
    private List<CategoryTreeNode> children = new ArrayList<>();

    public CategoryTreeNode() {}

    public CategoryTreeNode(Long id, Long parentId, String name, String slug,
                            String icon, int sortOrder, boolean visible) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.slug = slug;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public List<CategoryTreeNode> getChildren() { return children; }
    public void setChildren(List<CategoryTreeNode> children) { this.children = children; }
}
