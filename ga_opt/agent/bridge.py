import subprocess, json

JAR_NAME = 'ga_opt/target/optimizer-1.0.jar'

def call_java_optimizer(params, jar_path=JAR_NAME):
    process = subprocess.Popen(
        ['java', '-jar', jar_path],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    input_json = json.dumps(params)
    stdout, stderr = process.communicate(input_json)

    if stderr:
        print("Java error:", stderr)
    return stdout
    return json.loads(stdout)

out = call_java_optimizer(
            {
          "itemTypes": [
            {"number": 1, "width": 5.0, "height": 3.0, "price": 25.0, "quantity": 2}
          ],
          "binTypes": [
            {"number": 1, "width": 20.0, "height": 30.0}
          ]
        }
)

print(out)