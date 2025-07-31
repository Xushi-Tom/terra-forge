#!/bin/bash

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_banner() {
    echo -e "${BLUE}"
    echo "===================================="
    echo "    TerraForge GeoTiler 构建脚本"
    echo "===================================="
    echo -e "${NC}"
}

# 检查必要条件
check_requirements() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker未安装！请先安装Docker"
        exit 1
    fi
    
    if [ ! -f "docker/Dockerfile.geotiler" ]; then
        print_error "docker/Dockerfile.geotiler不存在！"
        exit 1
    fi
    
    if [ ! -d "cesium-terrain-builder" ]; then
        print_error "cesium-terrain-builder目录不存在！"
        exit 1
    fi
    
    print_success "环境检查通过"
}

# 构建镜像
build_image() {
    print_info "构建TerraForge GeoTiler镜像（使用本地CTB源码）..."
    
    # 清理旧镜像
    docker rmi terra-forge:release-1.0 2>/dev/null || true
    
    # 构建新镜像
    docker build -f docker/Dockerfile.geotiler -t terra-forge:release-1.0 .
    
    if [ $? -eq 0 ]; then
        print_success "镜像构建成功！镜像名: terra-forge:release-1.0"
    else
        print_error "镜像构建失败！"
        exit 1
    fi
}

# 测试镜像
test_image() {
    print_info "测试镜像工具..."
    
    # 测试GDAL
    docker run --rm terra-forge:release-1.0 gdalinfo --version | head -1
    
    # 测试CTB
    docker run --rm terra-forge:release-1.0 ctb-tile --version
    
    print_success "工具测试完成"
}

# 创建数据目录
create_directories() {
    print_info "创建数据目录..."
    mkdir -p data/dataSource data/tiles
    print_success "数据目录创建完成"
}

# 显示使用说明
show_usage() {
    print_success "构建完成！"
    echo ""
    echo "启动API服务容器："
    echo "  docker run -d --name terraforge \\"
    echo "    -p 8000:8000 \\"
    echo "    -v \/Users/xushi/TIF:/app/dataSource \\"
    echo "    -v \/Users/xushi/tiles:/app/tiles \\"
    echo "    -v \/Users/xushi/log:/app/log \\"
    echo "    terra-forge:release-1.0 python3 app.py"
    echo ""
    echo "或启动命令行容器："
    echo "  docker run -d --name terraforge-cli \\"
    echo "    -v \/Users/xushi/TIF:/app/dataSource \\"
    echo "    -v \/Users/xushi/tiles:/app/tiles \\"
    echo "    terra-forge:release-1.0 tail -f /dev/null"
    echo ""
    echo "使用工具："
    echo "  docker exec -it terraforge bash"
    echo "  docker exec terraforge ctb-tile -f Mesh -C -o /app/tiles/output /app/dataSource/input.tif"
    echo "  docker exec terraforge gdal2tiles.py /app/dataSource/input.tif /app/tiles/output/"
    echo ""
    echo "访问API服务："
    echo "  http://localhost:8000/api/health"
}

# 主函数
main() {
    print_banner
    
    check_requirements
    build_image
    test_image
    create_directories
    show_usage
}

# 执行
main "$@" 