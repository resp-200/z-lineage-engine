# z-lineage-engine

`z-lineage-engine` 是一个可被其他 Java 项目直接引用的 **Maven 多模块库**，用于在编译期分析业务操作关系并生成图谱。

## 项目定位

这个项目是一个标准 Maven 组件集合。业务项目通过依赖本项目发布的包，即可获得：

- 注解声明能力（业务操作、事件、资源触达）
- 编译期关系推理能力（先后关系、互斥关系）
- 图谱导出能力（JSON + Mermaid + HTML）

## 功能概览

### 1) 注解定义（lineage-annotations）

提供最小注解集：

- `@BizOp`：标记业务操作入口
- `@Effect`：描述操作证据（EMIT/CONSUME/TOUCH）
- `@RuleHint`：推理不足时人工兜底
- `@GraphIgnore`：忽略噪声方法

### 2) 编译期推理（lineage-processor）

在 `compile/process-classes` 阶段执行注解处理器，推导：

- `PRECEDES`：`EMITS(A,E)` + `CONSUMES(B,E)`
- `MUTEX`：同资源 + 同业务 key 双写冲突

默认告警不阻断（可配置）。

### 3) 图谱导出（lineage-processor）

输出文件：

- `target/classes/META-INF/lineage/graph.json`
- `target/lineage/graph.json`
- `target/lineage/graph.mmd`
- `target/lineage/graph.html`

### 4) 可运行示例（lineage-example）

示例模块演示如何接入注解和处理器，并在构建后验证图谱产物存在及内容正确。

## 模块结构

- `lineage-annotations`：注解与枚举定义
- `lineage-processor`：注解处理器、模型、导出器
- `lineage-example`：接入示例与测试

## 如何在业务项目中接入

### 1. 增加依赖

```xml
<dependency>
  <groupId>org.example</groupId>
  <artifactId>lineage-annotations</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置注解处理器

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.11.0</version>
  <executions>
    <execution>
      <id>lineage-process</id>
      <phase>process-classes</phase>
      <goals>
        <goal>compile</goal>
      </goals>
      <configuration>
        <proc>only</proc>
        <annotationProcessorPaths>
          <path>
            <groupId>org.example</groupId>
            <artifactId>lineage-processor</artifactId>
            <version>1.0-SNAPSHOT</version>
          </path>
        </annotationProcessorPaths>
        <compilerArgs>
          <arg>-Alineage.output=json,mermaid,html</arg>
          <arg>-Alineage.minConfidence=0.6</arg>
          <arg>-Alineage.strict=false</arg>
          <arg>-Alineage.outputDir=${project.build.directory}/lineage</arg>
        </compilerArgs>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## 本仓库本地验证

在仓库根目录执行：

```bash
mvn clean test
```

执行后可在 `lineage-example/target/lineage/` 查看：

- `graph.json`
- `graph.mmd`
- `graph.html`

## 当前版本边界（MVP）

- ✅ PRECEDES / MUTEX 推理
- ✅ JSON + Mermaid + HTML 输出
- ✅ 告警不阻断编译（默认）
- ⛔ 暂不包含复杂语义启发式冲突推理

## 5 分钟快速理解处理器

核心入口：`lineage.processor.LineageProcessor`

处理器整体分三步：

1. **收集阶段**（扫描 `@BizOp` 方法）
   - 读取 `@Effect`，先建立直接证据边：`EMITS`、`CONSUMES`、`TOUCHES`
   - 记录事件发出方、事件消费方、资源写冲突索引
2. **推理阶段**（编译最后一轮）
   - 规则 R1：`EMITS(A,E)` + `CONSUMES(B,E)` => `PRECEDES(A,B)`
   - 规则 R2：同资源 + 同 key 双写冲突 => `MUTEX(A,B)`
3. **导出阶段**
   - 导出 `graph.json` 与 `graph.mmd`
   - 按 `lineage.minConfidence` 过滤低置信边

## 注解最小使用示例

```java
@BizOp(id = "order.create", domain = "order")
@Effect(type = EffectType.TOUCH, target = "Order", access = AccessMode.WRITE, key = "orderId")
@Effect(type = EffectType.EMIT, target = "OrderCreated")
public void createOrder() {}

@BizOp(id = "order.pay", domain = "order")
@Effect(type = EffectType.CONSUME, target = "OrderCreated")
@Effect(type = EffectType.TOUCH, target = "Order", access = AccessMode.WRITE, key = "orderId")
public void payOrder() {}
```

这段会推导出：

- `order.create PRECEDES order.pay`
- 如果存在另一个对 `Order/orderId` 的写操作，则会推导 `MUTEX`

## graph.json 字段说明

`graph.json` 顶层结构：

- `nodes[]`：节点列表（`id/name/type`）
- `edges[]`：边列表（`from/to/type/confidence/evidence/...`）
- `warnings[]`：推理过程告警

常见边类型：

- `EMITS` / `CONSUMES` / `TOUCHES`（直接证据）
- `PRECEDES` / `MUTEX`（推理结果）

示例片段：

```json
{
  "from": "op:order.create",
  "to": "op:order.pay",
  "type": "PRECEDES",
  "confidence": 0.90,
  "evidence": "rule:event-chain"
}
```

## 常见问题

### 1) 为什么有 warning 但构建没失败？
默认 `-Alineage.strict=false`，告警不阻断。要严格模式可改为 `true`。

### 2) 为什么没生成 `MUTEX`？
通常是 `TOUCH` 没有 `WRITE`，或缺少 `key`，或同资源同 key 的双写条件不满足。

### 3) 为什么我看不到图文件？
先确认处理器执行配置在 `process-classes` 阶段，并检查：

- `target/lineage/graph.json`
- `target/lineage/graph.mmd`
