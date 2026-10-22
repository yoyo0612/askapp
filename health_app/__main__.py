import argparse

from .db import connect
from .server import create_server


def main():
    parser = argparse.ArgumentParser(description="食事記録・栄養計算アプリ")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--db", default="health.db", help="SQLite のファイルパス")
    args = parser.parse_args()

    conn = connect(args.db)
    server = create_server(conn, args.host, args.port)
    print(f"http://{args.host}:{args.port} で起動しました（Ctrl+C で停止）")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        conn.close()


if __name__ == "__main__":
    main()
