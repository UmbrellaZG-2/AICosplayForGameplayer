import requests
import json

# 设置基础URL
base_url = 'http://localhost:8080/api/auth'

# 步骤1: 登录获取会话状态
def login():
    url = f'{base_url}/login'
    headers = {'Content-Type': 'application/json'}
    payload = {
        'username': 'umbrellazg',
        'password': 'zhangge1121'
    }
    
    try:
        # 使用session来保持登录状态
        session = requests.Session()
        response = session.post(url, headers=headers, data=json.dumps(payload))
        print(f'登录 - 状态码: {response.status_code}')
        print(f'登录 - 响应: {response.text}')
        
        if response.status_code == 200:
            return session
        else:
            print('登录失败，无法继续测试')
            return None
    except Exception as e:
        print(f'登录 - 错误: {e}')
        return None

# 步骤2: 获取用户信息
def get_user_info(session):
    if not session:
        return
        
    url = f'{base_url}/user'
    
    try:
        response = session.get(url)
        print(f'\n获取用户信息 - 状态码: {response.status_code}')
        print(f'获取用户信息 - 响应: {response.text}')
    except Exception as e:
        print(f'获取用户信息 - 错误: {e}')

# 步骤3: 退出登录
def logout(session):
    if not session:
        return
        
    url = f'{base_url}/logout'
    
    try:
        response = session.post(url)
        print(f'\n退出登录 - 状态码: {response.status_code}')
        print(f'退出登录 - 响应: {response.text}')
    except Exception as e:
        print(f'退出登录 - 错误: {e}')

if __name__ == '__main__':
    print("=== 测试登录及用户信息功能 ===")
    
    # 登录获取session
    session = login()
    
    # 如果登录成功，继续测试其他功能
    if session:
        # 获取用户信息
        get_user_info(session)
        
        # 退出登录
        logout(session)