$password = "a029b7d039"
$sqlFile = "d:/Code/AICosplaying/20250924_insert_game_characters.sql"

# 尝试找到MySQL可执行文件
$mysqlPaths = @(
    "mysql", # 检查PATH
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe", # 常见安装路径
    "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$mysqlExecutable = $null
foreach ($path in $mysqlPaths) {
    if (Get-Command $path -ErrorAction SilentlyContinue) {
        $mysqlExecutable = $path
        break
    }
}

if ($mysqlExecutable) {
    Write-Host "找到MySQL可执行文件：$mysqlExecutable"
    
    try {
        # 使用start-process来执行命令，避免重定向问题
        $process = Start-Process -FilePath $mysqlExecutable `
                                -ArgumentList @("-u", "aicosplay", "-p$password", "aicosplay", "-e", "source $sqlFile") `
                                -NoNewWindow `
                                -Wait `
                                -PassThru
        
        if ($process.ExitCode -eq 0) {
            Write-Host "SQL脚本执行成功！"
        } else {
            Write-Host "SQL脚本执行失败，退出码：$($process.ExitCode)"
        }
    } catch {
        Write-Host "执行SQL脚本时发生错误：$($_.Exception.Message)"
    }
} else {
    Write-Host "未找到MySQL可执行文件，请确保MySQL已安装且在系统PATH中。"
    Write-Host "或者手动执行以下命令："
    Write-Host "mysql -u aicosplay -p aicosplay -e 'source d:/Code/AICosplaying/20250924_insert_game_characters.sql'"
    Write-Host "(输入密码：a029b7d039)"
}