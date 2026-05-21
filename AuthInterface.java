/**
 * 身份認證介面
 * 負責定義系統的認證規格，達成系統解耦。
 */
public interface AuthInterface {
    
    /**
     * 驗證使用者身份
     * * @param voterId 使用者編號
     * @param credential 認證憑證 (例如 PIN 碼或密碼)
     * @return 驗證成功回傳 true，失敗回傳 false
     */
    boolean authenticate(String voterId, String credential);
    
}