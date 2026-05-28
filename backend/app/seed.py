import asyncio

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import async_session
from app.core.security import hash_password
from app.models.organization import Organization
from app.models.user import User
from app.models.role import Role, Permission
from app.models.dictionary import DictionaryCategory, DictionaryItem, StatusTransition


async def seed_all():
    async with async_session() as db:
        existing = await db.execute(select(Organization).limit(1))
        if existing.scalar_one_or_none():
            print("数据库已有数据，跳过种子数据初始化")
            return

        print("开始初始化种子数据...")

        client_org = Organization(name="XX集团", code="CLIENT_HQ", org_type="CLIENT")
        vendor_org = Organization(name="我方团队", code="VENDOR_HQ", org_type="VENDOR")
        db.add_all([client_org, vendor_org])
        await db.flush()

        roles_data = [
            ("ADMIN", "系统管理员"),
            ("CLIENT_USER", "甲方用户"),
            ("CLIENT_MANAGER", "甲方管理者"),
            ("VENDOR_PRODUCT", "乙方产品"),
            ("VENDOR_DEV", "乙方研发"),
            ("VENDOR_TEST", "乙方测试"),
            ("VENDOR_MANAGER", "乙方管理者"),
        ]
        roles = {}
        for code, name in roles_data:
            r = Role(name=name, code=code)
            db.add(r)
            roles[code] = r
        await db.flush()

        admin = User(
            name="管理员", phone="13800000000", password_hash=hash_password("admin123"),
            org_id=vendor_org.id, user_type="VENDOR", vendor_role="ADMIN", status="ACTIVE",
        )
        db.add(admin)
        await db.flush()
        admin.roles.append(roles["ADMIN"])

        categories = {
            "PLATFORM": ("平台/端", [
                ("C_END", "C端（员工电商平台）", "#409EFF"),
                ("B_END", "B端（企业采购平台）", "#67C23A"),
                ("OPS_BACKEND", "运营管理后台", "#E6A23C"),
                ("SUPPLIER_PORTAL", "供应商协同门户", "#F56C6C"),
                ("ENTERPRISE_PORTAL", "企业自维护平台", "#909399"),
            ]),
            "ISSUE_TYPE": ("问题类型", [
                ("PENDING_EVAL", "待评估", "#909399"),
                ("BUG", "系统Bug", "#F56C6C"),
                ("DESIGN_DEFECT", "设计缺陷", "#E6A23C"),
                ("FEATURE_OPT", "功能优化", "#409EFF"),
                ("CONSULTATION", "咨询答疑", "#67C23A"),
                ("EXTERNAL_ISSUE", "外部系统问题", "#909399"),
            ]),
            "ISSUE_STATUS": ("问题状态", [
                ("SUBMITTED", "已提交", "#909399", {"phase": "open"}),
                ("EVALUATING", "待评估", "#E6A23C", {"phase": "open"}),
                ("PENDING", "待处理", "#F56C6C", {"phase": "open"}),
                ("DEV_IN_PROGRESS", "研发跟进中", "#409EFF", {"phase": "in_progress"}),
                ("PRODUCT_FOLLOW_UP", "产品跟进中", "#409EFF", {"phase": "in_progress"}),
                ("TESTING", "测试中", "#409EFF", {"phase": "in_progress"}),
                ("TEST_DONE_PENDING_VERIFY", "测试完成待产品验证", "#E6A23C", {"phase": "in_progress"}),
                ("VERIFIED_PENDING_RELEASE", "产品验证完成待上线", "#67C23A", {"phase": "in_progress"}),
                ("RELEASED", "已上线", "#67C23A", {"phase": "done"}),
                ("RESOLVED", "已解决", "#67C23A", {"phase": "closed"}),
                ("UNRESOLVED", "验证不通过", "#F56C6C", {"phase": "open"}),
            ]),
            "ISSUE_PRIORITY": ("优先级", [
                ("CRITICAL", "紧急", "#F56C6C"),
                ("HIGH", "高", "#E6A23C"),
                ("MEDIUM", "中", "#409EFF"),
                ("LOW", "低", "#909399"),
            ]),
            "AI_PROVIDER": ("AI服务商", [
                ("DEEPSEEK", "DeepSeek", None, {"base_url": "https://api.deepseek.com", "default_model": "deepseek-chat"}),
                ("QWEN", "通义千问", None, {"base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1", "default_model": "qwen-max"}),
            ]),
        }

        status_items = {}
        for cat_code, (cat_name, items) in categories.items():
            cat = DictionaryCategory(name=cat_name, code=cat_code, is_system=True)
            db.add(cat)
            await db.flush()

            for i, item_data in enumerate(items):
                code, name = item_data[0], item_data[1]
                color = item_data[2] if len(item_data) > 2 else None
                extra = item_data[3] if len(item_data) > 3 else None
                is_default = (cat_code == "ISSUE_TYPE" and code == "PENDING_EVAL") or \
                             (cat_code == "ISSUE_PRIORITY" and code == "MEDIUM") or \
                             (cat_code == "AI_PROVIDER" and code == "DEEPSEEK")
                item = DictionaryItem(
                    category_id=cat.id, name=name, code=code, color=color,
                    sort_order=i, is_default=is_default, is_system=True, extra_config=extra,
                )
                db.add(item)
                if cat_code == "ISSUE_STATUS":
                    await db.flush()
                    status_items[code] = item

        await db.flush()

        transitions = [
            ("SUBMITTED", "EVALUATING", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("EVALUATING", "PENDING", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("PENDING", "DEV_IN_PROGRESS", ["VENDOR_DEV", "VENDOR_MANAGER"], None),
            ("PENDING", "PRODUCT_FOLLOW_UP", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("DEV_IN_PROGRESS", "TESTING", ["VENDOR_DEV", "VENDOR_MANAGER"], None),
            ("DEV_IN_PROGRESS", "PRODUCT_FOLLOW_UP", ["VENDOR_DEV", "VENDOR_MANAGER"], None),
            ("PRODUCT_FOLLOW_UP", "DEV_IN_PROGRESS", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("PRODUCT_FOLLOW_UP", "VERIFIED_PENDING_RELEASE", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("TESTING", "TEST_DONE_PENDING_VERIFY", ["VENDOR_TEST", "VENDOR_MANAGER"], None),
            ("TESTING", "DEV_IN_PROGRESS", ["VENDOR_TEST", "VENDOR_MANAGER"], None),
            ("TEST_DONE_PENDING_VERIFY", "VERIFIED_PENDING_RELEASE", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("TEST_DONE_PENDING_VERIFY", "TESTING", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
            ("VERIFIED_PENDING_RELEASE", "RELEASED", ["VENDOR_PRODUCT", "VENDOR_DEV", "VENDOR_MANAGER"], None),
            ("RELEASED", "RESOLVED", [], "SUBMITTER_ONLY"),
            ("RELEASED", "UNRESOLVED", [], "SUBMITTER_ONLY"),
            ("UNRESOLVED", "PENDING", ["VENDOR_PRODUCT", "VENDOR_MANAGER"], None),
        ]

        for from_code, to_code, roles_list, rule in transitions:
            t = StatusTransition(
                from_status_id=status_items[from_code].id,
                to_status_id=status_items[to_code].id,
                allowed_roles=roles_list,
                special_rule=rule,
            )
            db.add(t)

        await db.commit()
        print("种子数据初始化完成！")
        print(f"  管理员账号: 13800000000 / admin123")


if __name__ == "__main__":
    asyncio.run(seed_all())
