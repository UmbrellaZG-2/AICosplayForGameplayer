import requests
import json

url = 'http://localhost:8080/api/auth/login'
headers = {'Content-Type': 'application/json'}
payload = {
    'username': 'umbrellazg',
    'password': 'zhangge1121'
}

try:
    response = requests.post(url, headers=headers, data=json.dumps(payload))
    print(f'Status Code: {response.status_code}')
    print(f'Response: {response.text}')
except Exception as e:
    print(f'Error: {e}')