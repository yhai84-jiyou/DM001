# 图片轮播 Flask Demo

一个最小化的 Python/Flask 项目，用于展示本地图片的轮播图效果。只要把图片放进 `src/static/images`，页面就会自动载入。

## 环境要求

- Python 3.10+
- pip / venv 工具（推荐）

## 快速开始

```bash
# 创建虚拟环境（可选，但推荐）
python -m venv .venv
source .venv/bin/activate  # Windows 下使用 .venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 启动开发服务器
python src/app.py
```

启动后访问 <http://127.0.0.1:5000> 即可看到轮播效果。

## 添加或替换图片

1. 将 JPG/PNG/GIF/WebP 文件复制到 `src/static/images/`。
2. 刷新浏览器，新的图片会按照文件名排序出现在轮播中。
3. 示例中已经包含 3 张占位图，方便直接预览。

## 项目结构

```
.
├── README.md
├── requirements.txt
└── src
    ├── app.py
    ├── static
    │   ├── css
    │   │   └── styles.css
    │   └── images
    │       ├── sample1.jpg
    │       ├── sample2.jpg
    │       └── sample3.jpg
    └── templates
        └── index.html
```

## 自定义

- 在 `static/css/styles.css` 中调整布局、配色或动画。
- 修改 `templates/index.html` 以整合到现有站点或添加文案。
- 如需部署，可将 Flask 应用挂到任何支持 WSGI 的平台（Gunicorn、uWSGI 等）。
