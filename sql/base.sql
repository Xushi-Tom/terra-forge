CREATE TABLE "cim"."cim_bas_map_record"
(
    "id" VARCHAR(50) NOT NULL,
    "file_path" VARCHAR(255),
    "file_size" VARCHAR(50),
    "file_type" VARCHAR(50),
    "file_name" VARCHAR(50),
    "zoom_min" BYTE,
    "zoom_max" BYTE,
    "type" BYTE,
    "output_path" VARCHAR(50),
    "workspace_group" VARCHAR(50),
    "workspace" VARCHAR(50),
    "start_time" TIMESTAMP(6),
    "end_time" TIMESTAMP(6),
    "create_time" TIMESTAMP(6),
    NOT CLUSTER PRIMARY KEY("id")) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "cim"."cim_bas_map_record" IS '地图服务解析记录表';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."create_time" IS '创建时间';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."end_time" IS '结束时间';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."file_name" IS '文件名称';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."file_path" IS '文件路径';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."file_size" IS '文件大小';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."file_type" IS '文件类型 1：电子 2：遥感';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."output_path" IS '输出路径';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."start_time" IS '开始时间';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."type" IS '解析类型 1：地图 2：地形';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."workspace" IS '工作空间';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."workspace_group" IS '工作空间组名称';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."zoom_max" IS '解析的最大层级';

COMMENT ON COLUMN "cim"."cim_bas_map_record"."zoom_min" IS '解析的最小层级';

CREATE TABLE "cim"."cim_bas_workspace"
(
    "id" VARCHAR(50) NOT NULL,
    "parent_id" VARCHAR(50),
    "name" VARCHAR(50),
    "type" BYTE,
    "status" BYTE,
    "path" VARCHAR(50),
    "create_time" TIMESTAMP(6),
    NOT CLUSTER PRIMARY KEY("id")) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "cim"."cim_bas_workspace" IS '工作空间记录表';

COMMENT ON COLUMN "cim"."cim_bas_workspace"."create_time" IS '创建时间';

COMMENT ON COLUMN "cim"."cim_bas_workspace"."name" IS '工作空间/组名称';

COMMENT ON COLUMN "cim"."cim_bas_workspace"."parent_id" IS '父级 id';

COMMENT ON COLUMN "cim"."cim_bas_workspace"."path" IS '工作空间/组完整路径';

COMMENT ON COLUMN "cim"."cim_bas_workspace"."status" IS '状态 0：停用 1：启动';

COMMENT ON COLUMN "cim"."cim_bas_workspace"."type" IS '类型 1：空间组 2：空间';

CREATE TABLE "cim"."CIM_BAS_TASK"
(
    "ID" VARCHAR(50) NOT NULL,
    "TASK_NAME" VARCHAR(255),
    "FILE_NAME" VARCHAR(255),
    "FILE_PATH" VARCHAR(500),
    "WORKSPACE_GROUP" VARCHAR(100),
    "WORKSPACE" VARCHAR(100),
    "TASK_TYPE" VARCHAR(20),
    "STATUS" VARCHAR(20),
    "PROGRESS" INT,
    "MIN_ZOOM" INT,
    "MAX_ZOOM" INT,
    "OUTPUT_PATH" VARCHAR(500),
    "ERROR_MESSAGE" TEXT,
    "START_TIME" TIMESTAMP(6),
    "END_TIME" TIMESTAMP(6),
    "CREATE_TIME" TIMESTAMP(6),
    "UPDATE_TIME" TIMESTAMP(6),
    NOT CLUSTER PRIMARY KEY("ID")
) STORAGE(ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "cim"."CIM_BAS_TASK" IS '任务管理表';

COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."ID" IS '主键ID';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."TASK_NAME" IS '任务名称';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."FILE_NAME" IS '文件名';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."FILE_PATH" IS '文件路径';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."WORKSPACE_GROUP" IS '工作空间组';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."WORKSPACE" IS '工作空间';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."TASK_TYPE" IS '任务类型：TMS/TERRAIN';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."STATUS" IS '任务状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."PROGRESS" IS '进度百分比';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."MIN_ZOOM" IS '最小缩放级别';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."MAX_ZOOM" IS '最大缩放级别';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."OUTPUT_PATH" IS '输出路径';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."ERROR_MESSAGE" IS '错误信息';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."START_TIME" IS '开始时间';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."END_TIME" IS '结束时间';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."CREATE_TIME" IS '创建时间';
COMMENT ON COLUMN "cim"."CIM_BAS_TASK"."UPDATE_TIME" IS '更新时间';

