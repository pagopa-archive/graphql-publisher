package it.pagopa.dbtographql.common

import java.io.{File, FileNotFoundException, FileReader, IOException}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import org.bouncycastle.util.io.pem.PemReader

//Copyright 2017 - https://github.com/lbalmaceda
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw"
  )
)
object PemUtils {
  @throws[IOException]
  private def parsePEMFile(pemFile: File): Array[Byte] = {
    if (!pemFile.isFile || !pemFile.exists) throw new FileNotFoundException(String.format("The file '%s' doesn't exist.", pemFile.getAbsolutePath))
    val reader = new PemReader(new FileReader(pemFile))
    val pemObject = reader.readPemObject
    val content = pemObject.getContent
    reader.close()
    content
  }

  private def getPublicKey(keyBytes: Array[Byte], algorithm: String): PublicKey = {
    val kf = KeyFactory.getInstance(algorithm)
    val keySpec = new X509EncodedKeySpec(keyBytes)
    kf.generatePublic(keySpec)
  }

  private def getPrivateKey(keyBytes: Array[Byte], algorithm: String): PrivateKey = {
    val kf = KeyFactory.getInstance(algorithm)
    val keySpec = new PKCS8EncodedKeySpec(keyBytes)
    kf.generatePrivate(keySpec)
  }

  @throws[IOException]
  def readPublicKeyFromFile(filepath: String, algorithm: String): PublicKey = {
    val bytes = PemUtils.parsePEMFile(new File(filepath))
    PemUtils.getPublicKey(bytes, algorithm)
  }

  @throws[IOException]
  def readPrivateKeyFromFile(filepath: String, algorithm: String): PrivateKey = {
    val bytes = PemUtils.parsePEMFile(new File(filepath))
    PemUtils.getPrivateKey(bytes, algorithm)
  }
}
