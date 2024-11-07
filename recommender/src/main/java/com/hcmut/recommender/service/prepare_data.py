import pandas as pd
import numpy as np
from sqlalchemy import create_engine

# Kết nối đến PostgreSQL
connection_string = 'postgresql://stream_admin:abc12345@100.85.204.98:5432/streamconnect'
engine = create_engine(connection_string)

# Lấy dữ liệu từ bảng recommender
query = "SELECT user_id, livestream_id, category_id, shop_id, created_at, event_time, views FROM recommender"
df = pd.read_sql(query, engine)

# Tính toán các chỉ số và rating

# Tần suất người dùng xem livestream từng category
df['view_count'] = df.groupby(['user_id', 'category_id'])['livestream_id'].transform('count')

# Tần suất người dùng xem livestream từng shop
df['shop_frequency'] = df.groupby(['user_id', 'shop_id'])['livestream_id'].transform('count')

# Tính freshness cho mỗi shop dựa trên thời gian mở (created_at) và số lượng views của shop
df['created_at_days'] = (pd.to_datetime('now') - pd.to_datetime(df['created_at'])).dt.days
df['freshness'] = 1 / (1 + np.log1p(df['created_at_days']))
df['shop_views'] = df.groupby('shop_id')['views'].transform('sum')

# Chuẩn hóa freshness
df['shop_freshness_norm'] = df['freshness'] * np.log1p(df['shop_views'])
df['shop_freshness_norm'] = df['shop_freshness_norm'] / df['shop_freshness_norm'].max()

# Chuẩn hóa view_count
df['view_count_log'] = np.log1p(df['view_count'])
df['view_count_norm'] = (df['view_count_log'] - df['view_count_log'].min()) / (df['view_count_log'].max() - df['view_count_log'].min())

# Chuẩn hóa shop_frequency
df['shop_frequency_log'] = np.log1p(df['shop_frequency'])
df['shop_frequency_norm'] = (df['shop_frequency_log'] - df['shop_frequency_log'].min()) / (df['shop_frequency_log'].max() - df['shop_frequency_log'].min())

# Tính toán rating
alpha = 0.3
beta = 0.2
gamma = 0.5

df['rating'] = alpha * df['view_count_norm'] + beta * df['shop_frequency_norm'] + gamma * df['shop_freshness_norm']

# Tạo bảng mới recommender_rating nếu chưa tồn tại
df_rating = df[['user_id', 'livestream_id', 'rating']]

# Lưu kết quả vào bảng mới 'recommender_rating'
df_rating.to_sql(
    'recommender_rating',
    engine,
    if_exists='replace',  # Hoặc 'append' nếu bạn muốn thêm vào bảng đã tồn tại
    index=False
)

print("Da luu rating vao bang recommender_rating cua database")
