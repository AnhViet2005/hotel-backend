Write-Host "\n--- 20 dòng cuối của mysql_error.log ---\n"
Get-Content "C:\\xampp\\mysql\\data\\mysql_error.log" -Tail 20

# Kiểm tra cổng 3306
$pid = netstat -ano | Select-String ':3306' | Where-Object { $_ -match 'LISTENING' } | ForEach-Object { ($_ -split '\s+')[4] } | Select-Object -First 1
if ($pid) {
    Write-Host "\nCổng 3306 đang được tiến trình PID $pid sử dụng. Đang dừng tiến trình..."
    Stop-Process -Id $pid -Force
} else {
    Write-Host "\nCổng 3306 hiện đang trống."
}

Write-Host "\n--- Xóa các file PID / lock cũ ---\n"
# Dừng dịch vụ MySQL nếu đang chạy (trong XAMPP thường không phải service, nhưng gọi để an toàn)
Stop-Service -Name "mysql" -ErrorAction SilentlyContinue
Remove-Item "C:\\xampp\\mysql\\data\\mysql.pid" -Force -ErrorAction SilentlyContinue
Remove-Item "C:\\xampp\\mysql\\data\\ib_logfile0" -Force -ErrorAction SilentlyContinue
Remove-Item "C:\\xampp\\mysql\\data\\ib_logfile1" -Force -ErrorAction SilentlyContinue

# Đổi cổng sang 3307 nếu cần
$myIni = "C:\\xampp\\mysql\\bin\\my.ini"
if (Select-String -Path $myIni -Pattern "port=3306") {
    (Get-Content $myIni) -replace "port=3306", "port=3307" | Set-Content $myIni
    Write-Host "\nĐã thay đổi cổng MySQL thành 3307 trong my.ini"
} else {
    Write-Host "\nKhông tìm thấy dòng 'port=3306' trong my.ini, bỏ qua bước đổi cổng."
}

Write-Host "\n--- Khởi động MySQL qua XAMPP Control Panel ---\n"
Start-Process "C:\\xampp\\xampp-control.exe" -ArgumentList "/start mysql"
