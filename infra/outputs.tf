# apply 後にターミナルへ表示する値。`terraform -chdir=infra output` で再表示できる。

output "app_public_ip" {
  description = "アプリEC2の固定パブリックIP（Elastic IP・DuckDNS が指す先）"
  value       = aws_eip.app.public_ip
}

output "app_http_url" {
  description = "疎通確認用の一次URL（TLS 化前）"
  value       = "http://${aws_eip.app.public_ip}/"
}

output "app_instance_id" {
  description = "EC2 インスタンスID（SSM Session Manager の接続先）"
  value       = aws_instance.app.id
}

output "session_manager_command" {
  description = "SSH 鍵なしで箱へ入る（SSM Session Manager）"
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.region}"
}

output "rds_address" {
  description = "RDS ホスト名（プライベート・EC2 からのみ到達）"
  value       = aws_db_instance.main.address
}
