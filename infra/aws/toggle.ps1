# Ban PowerShell cua toggle.sh (dung khi may khong co Git Bash trong PATH hoac "bash" bi WSL chiem).
# Dung: .\toggle.ps1 stop | .\toggle.ps1 start
#
# stop:  scale 10 ECS service (8 app + mongo + kafka) ve desired_count=0 + dung RDS
#        -> het tien ECS Fargate ngay, RDS chi con phi storage (~$2/thang cho 20GB gp3).
# start: bat RDS truoc (mat 2-5 phut "available"), roi scale ECS service lai ve 1.
#
# LUU Y: AWS tu dong BAT LAI RDS da stop sau 7 ngay (gioi han cung, khong tat duoc) - neu de quen
# hon 1 tuan, RDS se tu chay lai va tinh phi, ALB (~$16/thang) khong tat duoc bang cach nay - muon
# het ca phi ALB thi phai terraform destroy.

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("start", "stop")]
    [string]$Action
)

$ErrorActionPreference = "Stop"
$Cluster = "dishcover"
$DbInstance = "dishcover-pg"
$Services = @("gateway", "user", "inventory", "recipe", "matching", "rag", "image", "notification", "mongo", "kafka")

if ($Action -eq "stop") {
    Write-Host "Scale $($Services.Count) ECS service ve 0..."
    foreach ($svc in $Services) {
        aws ecs update-service --cluster $Cluster --service $svc --desired-count 0 | Out-Null
        Write-Host "  - $svc -> 0"
    }
    Write-Host "Dung RDS $DbInstance..."
    aws rds stop-db-instance --db-instance-identifier $DbInstance | Out-Null
    Write-Host "Xong. ALB van chay (khong tat duoc theo cach nay, ~`$0.53/ngay) - dung 'terraform destroy' neu can het sach phi."
}
else {
    Write-Host "Khoi dong RDS $DbInstance (mat vai phut)..."
    aws rds start-db-instance --db-instance-identifier $DbInstance | Out-Null
    aws rds wait db-instance-available --db-instance-identifier $DbInstance
    Write-Host "RDS available. Scale $($Services.Count) ECS service ve 1..."
    foreach ($svc in $Services) {
        aws ecs update-service --cluster $Cluster --service $svc --desired-count 1 | Out-Null
        Write-Host "  - $svc -> 1"
    }
    Write-Host "Doi service on dinh (co the mat 2-3 phut, Spring Boot khoi dong)..."
    aws ecs wait services-stable --cluster $Cluster --services $Services
    Write-Host "Xong. Base URL: xem 'terraform output alb_dns_name'."
}
