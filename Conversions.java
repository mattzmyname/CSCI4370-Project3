
public class Conversions
{
	public static byte [] short2ByteArray(short val)
	{
		byte [] b = new byte [] {(byte) (val >>> 8),
								 (byte) (val)};
		return b;
	}

	public static byte [] int2ByteArray(int val)
	{
		byte [] b = new byte [] {(byte) (val >>> 24),
								 (byte) (val >>> 16),
								 (byte) (val >>> 8),
								 (byte) (val)};
		return b;
	}

	public static byte [] long2ByteArray(long val)
	{
		byte [] b = new byte [] {(byte) (val >>> 56),
								 (byte) (val >>> 48),
								 (byte) (val >>> 40),
								 (byte) (val >>> 32),
								 (byte) (val >>> 24),
								 (byte) (val >>> 16),
								 (byte) (val >>> 8),
								 (byte) (val)};
		return b;
	}

	public static byte [] float2ByteArray(float val)
	{
		byte [] b = new byte [] {(byte) (Float.floatToRawIntBits(val) >>> 24),
								 (byte) (Float.floatToRawIntBits(val) >>> 16),
								 (byte) (Float.floatToRawIntBits(val) >>> 8),
								 (byte) Float.floatToRawIntBits(val)};	
		return b;
	}

	public static byte [] double2ByteArray(double val)
    {
        return new byte [] { (byte) (Double.doubleToRawLongBits(val) >>> 56),
                             (byte) (Double.doubleToRawLongBits(val) >>> 48),
                             (byte) (Double.doubleToRawLongBits(val) >>> 40),
                             (byte) (Double.doubleToRawLongBits(val) >>> 32),
                             (byte) (Double.doubleToRawLongBits(val) >>> 24),
                             (byte) (Double.doubleToRawLongBits(val) >>> 16),
                             (byte) (Double.doubleToRawLongBits(val) >>> 8),
                             (byte) Double.doubleToRawLongBits(val) };
    }
}