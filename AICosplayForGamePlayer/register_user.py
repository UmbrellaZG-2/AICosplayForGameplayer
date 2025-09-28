import requests
import json

# 设置基础URL
base_url = 'http://localhost:8080/api/auth'
username = 'umbrellzg'
password = 'zhangge1121'
email = 'umbrellzg@example.com'  # 假邮箱，因为实际不会发送邮件

# 步骤1: 生成验证码
def generate_verification_code():
    url = f'{base_url}/generate-code'
    headers = {'Content-Type': 'application/json'}
    payload = {'email': email}
    
    try:
        response = requests.post(url, headers=headers, data=json.dumps(payload))
        print(f'生成验证码 - 状态码: {response.status_code}')
        print(f'生成验证码 - 响应: {response.text}')
        return True
    except Exception as e:
        print(f'生成验证码 - 错误: {e}')
        return False

# 步骤2: 注册用户
def register_user(verification_code):
    url = f'{base_url}/register'
    headers = {'Content-Type': 'application/json'}
    payload = {
        'username': username,
        'email': email,
        'password': password,
        'verificationCode': verification_code
    }
    
    try:
        response = requests.post(url, headers=headers, data=json.dumps(payload))
        print(f'注册用户 - 状态码: {response.status_code}')
        print(f'注册用户 - 响应: {response.text}')
        return response.status_code == 200
    except Exception as e:
        print(f'注册用户 - 错误: {e}')
        return False

# 步骤3: 登录测试
def login_test():
    url = f'{base_url}/login'
    headers = {'Content-Type': 'application/json'}
    payload = {
        'username': username,
        'password': password
    }
    
    try:
        response = requests.post(url, headers=headers, data=json.dumps(payload))
        print(f'登录测试 - 状态码: {response.status_code}')
        print(f'登录测试 - 响应: {response.text}')
        return response.status_code == 200
    except Exception as e:
        print(f'登录测试 - 错误: {e}')
        return False

if __name__ == '__main__':
    # 步骤1: 生成验证码
    print("\n=== 步骤1: 生成验证码 ===")
    generate_verification_code()
    
    # 注意：在实际运行时，需要查看后端控制台输出获取生成的验证码
    print("\n请查看后端服务器控制台，获取生成的验证码。")
    print("提示：后端日志中会有类似'生成的验证码：XXXXXX 用于邮箱：umbrellzg@example.com'的输出")