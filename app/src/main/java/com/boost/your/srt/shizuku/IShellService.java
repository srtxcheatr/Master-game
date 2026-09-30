package com.boost.your.srt.shizuku;

public interface IShellService extends android.os.IInterface {
    public static final String DESCRIPTOR = "com.boost.your.srt.shizuku.IShellService";

    public void destroy() throws android.os.RemoteException;
    public String[] exec(String command) throws android.os.RemoteException;

    public static abstract class Stub extends android.os.Binder implements IShellService {
        static final int TRANSACTION_destroy = 16777114;
        static final int TRANSACTION_exec = 1;

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IShellService asInterface(android.os.IBinder obj) {
            if (obj == null) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IShellService) {
                return (IShellService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(DESCRIPTOR);
            }
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            switch (code) {
                case TRANSACTION_destroy: {
                    this.destroy();
                    return true;
                }
                case TRANSACTION_exec: {
                    java.lang.String _arg0 = data.readString();
                    java.lang.String[] _result = this.exec(_arg0);
                    if (reply != null) {
                        reply.writeNoException();
                        reply.writeStringArray(_result);
                    }
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements IShellService {
            private final android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void destroy() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_destroy, _data, null, android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public String[] exec(String command) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String[] _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(command);
                    mRemote.transact(TRANSACTION_exec, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createStringArray();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}
